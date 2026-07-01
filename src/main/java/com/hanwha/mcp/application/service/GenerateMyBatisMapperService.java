package com.hanwha.mcp.application.service;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.hanwha.mcp.application.dto.MyBatisGenerationOperation;
import com.hanwha.mcp.application.dto.MyBatisGeneratorDefaults;
import com.hanwha.mcp.application.dto.MyBatisMapperGenerationQuery;
import com.hanwha.mcp.application.usecase.GenerateMyBatisMapperUseCase;
import com.hanwha.mcp.domain.model.DatabaseColumnMetadata;
import com.hanwha.mcp.domain.model.GeneratedFileArtifact;
import com.hanwha.mcp.domain.model.MyBatisColumnMapping;
import com.hanwha.mcp.domain.model.MyBatisMapperGeneration;
import com.hanwha.mcp.domain.repository.DatabaseSchemaInspector;

public class GenerateMyBatisMapperService implements GenerateMyBatisMapperUseCase {

	private static final String JAVA_EXTENSION = ".java";
	private static final String XML_EXTENSION = ".xml";

	private final DatabaseSchemaInspector databaseSchemaInspector;
	private final MyBatisGeneratorDefaults defaults;

	public GenerateMyBatisMapperService(DatabaseSchemaInspector databaseSchemaInspector, MyBatisGeneratorDefaults defaults) {
		this.databaseSchemaInspector = databaseSchemaInspector;
		this.defaults = defaults;
	}

	@Override
	public MyBatisMapperGeneration generate(MyBatisMapperGenerationQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		var effectiveSchema = firstNonBlank(query.schemaName(), this.defaults.defaultSchema());
		var table = this.databaseSchemaInspector.inspectTable(effectiveSchema, query.tableName());
		var basePackage = firstNonBlank(query.basePackage(), this.defaults.basePackage());
		var domainObjectName = firstNonBlank(query.domainObjectName(), toPascalCase(table.tableName()));
		var dtoClassName = domainObjectName.endsWith("Dto") ? domainObjectName : domainObjectName + "Dto";
		var mapperInterfaceName = domainObjectName.endsWith("Mapper") ? domainObjectName : domainObjectName + "Mapper";
		var dtoPackage = firstNonBlank(query.dtoPackage(), basePackage + ".dto");
		var mapperPackage = firstNonBlank(query.mapperPackage(), basePackage + ".mapper");
		var mapperNamespace = mapperPackage + "." + mapperInterfaceName;
		var columns = table.columns().stream().map(this::toColumnMapping).toList();
		var warnings = new ArrayList<String>();
		addIdentifierWarnings(table.schemaName(), table.tableName(), columns, warnings);
		var files = List.of(
			new GeneratedFileArtifact(
				"dto",
				dtoClassName + JAVA_EXTENSION,
				toPath(dtoPackage, dtoClassName + JAVA_EXTENSION),
				"java",
				generateDto(dtoPackage, dtoClassName, columns)),
			new GeneratedFileArtifact(
				"mapper-interface",
				mapperInterfaceName + JAVA_EXTENSION,
				toPath(mapperPackage, mapperInterfaceName + JAVA_EXTENSION),
				"java",
				generateMapperInterface(mapperPackage, mapperInterfaceName, dtoPackage, dtoClassName, domainObjectName, columns, query.operations(), warnings)),
			new GeneratedFileArtifact(
				"mapper-xml",
				mapperInterfaceName + XML_EXTENSION,
				"src/main/resources/mapper/" + mapperInterfaceName + XML_EXTENSION,
				"xml",
				generateMapperXml(mapperNamespace, dtoPackage + "." + dtoClassName, table.schemaName(), table.tableName(), domainObjectName, columns, query.operations(), warnings)));
		return new MyBatisMapperGeneration(
			table.databaseProductName(),
			table.databaseProductVersion(),
			table.schemaName(),
			table.tableName(),
			domainObjectName,
			dtoClassName,
			mapperInterfaceName,
			mapperNamespace,
			columns,
			files,
			warnings);
	}

	private MyBatisColumnMapping toColumnMapping(DatabaseColumnMetadata column) {
		var javaType = resolveJavaType(column);
		return new MyBatisColumnMapping(
			column.name(),
			toCamelCase(column.name()),
			resolveJdbcType(column),
			javaType.simpleName(),
			javaType.importName(),
			column.nullable(),
			column.primaryKey(),
			column.autoIncrement(),
			column.ordinalPosition());
	}

	private String generateDto(String dtoPackage, String dtoClassName, List<MyBatisColumnMapping> columns) {
		var imports = columns.stream()
			.map(MyBatisColumnMapping::javaImport)
			.filter(Objects::nonNull)
			.collect(Collectors.toCollection(TreeSet::new));
		var builder = new StringBuilder();
		builder.append("package ").append(dtoPackage).append(";\n\n");
		for (String importName : imports) {
			builder.append("import ").append(importName).append(";\n");
		}
		if (!imports.isEmpty()) {
			builder.append("\n");
		}
		builder.append("public class ").append(dtoClassName).append(" {\n\n");
		for (MyBatisColumnMapping column : columns) {
			builder.append("\tprivate ").append(column.javaType()).append(" ").append(column.propertyName()).append(";\n");
		}
		builder.append("\n\tpublic ").append(dtoClassName).append("() {\n\t}\n\n");
		builder.append("\tpublic ").append(dtoClassName).append("(");
		builder.append(columns.stream()
			.map(column -> column.javaType() + " " + column.propertyName())
			.collect(Collectors.joining(", ")));
		builder.append(") {\n");
		for (MyBatisColumnMapping column : columns) {
			builder.append("\t\tthis.").append(column.propertyName()).append(" = ").append(column.propertyName()).append(";\n");
		}
		builder.append("\t}\n\n");
		for (MyBatisColumnMapping column : columns) {
			var methodSuffix = toPascalCase(column.propertyName());
			builder.append("\tpublic ").append(column.javaType()).append(" get").append(methodSuffix).append("() {\n")
				.append("\t\treturn this.").append(column.propertyName()).append(";\n")
				.append("\t}\n\n");
			builder.append("\tpublic void set").append(methodSuffix).append("(")
				.append(column.javaType()).append(" ").append(column.propertyName()).append(") {\n")
				.append("\t\tthis.").append(column.propertyName()).append(" = ").append(column.propertyName()).append(";\n")
				.append("\t}\n\n");
		}
		builder.append("}\n");
		return builder.toString();
	}

	private String generateMapperInterface(
			String mapperPackage,
			String mapperInterfaceName,
			String dtoPackage,
			String dtoClassName,
			String domainObjectName,
			List<MyBatisColumnMapping> columns,
			Set<MyBatisGenerationOperation> operations,
			List<String> warnings) {
		var primaryKeys = primaryKeys(columns);
		var effectiveOperations = supportedOperations(operations, primaryKeys, columns, warnings);
		var imports = new TreeSet<String>();
		imports.add(dtoPackage + "." + dtoClassName);
		imports.add("org.apache.ibatis.annotations.Mapper");
		if (effectiveOperations.contains(MyBatisGenerationOperation.SELECT_ALL)) {
			imports.add("java.util.List");
		}
		if (effectiveOperations.contains(MyBatisGenerationOperation.SELECT_BY_ID)) {
			imports.add("java.util.Optional");
		}
		if (primaryKeys.size() > 1 && hasPrimaryKeyOperation(effectiveOperations)) {
			imports.add("org.apache.ibatis.annotations.Param");
		}
		var builder = new StringBuilder();
		builder.append("package ").append(mapperPackage).append(";\n\n");
		for (String importName : imports) {
			builder.append("import ").append(importName).append(";\n");
		}
		builder.append("\n@Mapper\n");
		builder.append("public interface ").append(mapperInterfaceName).append(" {\n\n");
		if (effectiveOperations.contains(MyBatisGenerationOperation.SELECT_BY_ID)) {
			builder.append("\tOptional<").append(dtoClassName).append("> findById(").append(primaryKeyParameters(primaryKeys))
				.append(");\n\n");
		}
		if (effectiveOperations.contains(MyBatisGenerationOperation.SELECT_ALL)) {
			builder.append("\tList<").append(dtoClassName).append("> findAll();\n\n");
		}
		if (effectiveOperations.contains(MyBatisGenerationOperation.INSERT)) {
			builder.append("\tint insert(").append(dtoClassName).append(" ").append(toCamelCase(domainObjectName)).append(");\n\n");
		}
		if (effectiveOperations.contains(MyBatisGenerationOperation.UPDATE_BY_ID)) {
			builder.append("\tint update(").append(dtoClassName).append(" ").append(toCamelCase(domainObjectName)).append(");\n\n");
		}
		if (effectiveOperations.contains(MyBatisGenerationOperation.DELETE_BY_ID)) {
			builder.append("\tint deleteById(").append(primaryKeyParameters(primaryKeys)).append(");\n\n");
		}
		builder.append("}\n");
		return builder.toString();
	}

	private String generateMapperXml(
			String mapperNamespace,
			String dtoType,
			String schemaName,
			String tableName,
			String domainObjectName,
			List<MyBatisColumnMapping> columns,
			Set<MyBatisGenerationOperation> operations,
			List<String> warnings) {
		var primaryKeys = primaryKeys(columns);
		var effectiveOperations = supportedOperations(operations, primaryKeys, columns, warnings);
		var tableExpression = tableExpression(schemaName, tableName);
		var resultMapId = domainObjectName + "ResultMap";
		var builder = new StringBuilder();
		builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
		builder.append("<!DOCTYPE mapper\n");
		builder.append("  PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\"\n");
		builder.append("  \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n\n");
		builder.append("<mapper namespace=\"").append(mapperNamespace).append("\">\n\n");
		builder.append("\t<resultMap id=\"").append(resultMapId).append("\" type=\"").append(dtoType).append("\">\n");
		for (MyBatisColumnMapping column : columns) {
			builder.append("\t\t<").append(column.primaryKey() ? "id" : "result")
				.append(" column=\"").append(column.columnName())
				.append("\" property=\"").append(column.propertyName())
				.append("\" jdbcType=\"").append(column.jdbcType())
				.append("\" />\n");
		}
		builder.append("\t</resultMap>\n\n");
		builder.append("\t<sql id=\"BaseColumnList\">\n");
		builder.append("\t\t").append(columns.stream().map(MyBatisColumnMapping::columnName).collect(Collectors.joining(", "))).append("\n");
		builder.append("\t</sql>\n\n");
		if (effectiveOperations.contains(MyBatisGenerationOperation.SELECT_BY_ID)) {
			builder.append("\t<select id=\"findById\" resultMap=\"").append(resultMapId).append("\">\n");
			builder.append("\t\tSELECT <include refid=\"BaseColumnList\" />\n");
			builder.append("\t\tFROM ").append(tableExpression).append("\n");
			builder.append("\t\tWHERE ").append(whereClause(primaryKeys)).append("\n");
			builder.append("\t</select>\n\n");
		}
		if (effectiveOperations.contains(MyBatisGenerationOperation.SELECT_ALL)) {
			builder.append("\t<select id=\"findAll\" resultMap=\"").append(resultMapId).append("\">\n");
			builder.append("\t\tSELECT <include refid=\"BaseColumnList\" />\n");
			builder.append("\t\tFROM ").append(tableExpression).append("\n");
			builder.append("\t</select>\n\n");
		}
		if (effectiveOperations.contains(MyBatisGenerationOperation.INSERT)) {
			var insertableColumns = columns.stream().filter(column -> !column.autoIncrement()).toList();
			builder.append("\t<insert id=\"insert\" parameterType=\"").append(dtoType).append("\"");
			singleGeneratedKey(columns).ifPresent(column -> builder.append(" useGeneratedKeys=\"true\" keyProperty=\"")
				.append(column.propertyName()).append("\""));
			builder.append(">\n");
			builder.append("\t\tINSERT INTO ").append(tableExpression).append(" (")
				.append(insertableColumns.stream().map(MyBatisColumnMapping::columnName).collect(Collectors.joining(", ")))
				.append(")\n");
			builder.append("\t\tVALUES (")
				.append(insertableColumns.stream().map(this::parameter).collect(Collectors.joining(", ")))
				.append(")\n");
			builder.append("\t</insert>\n\n");
		}
		if (effectiveOperations.contains(MyBatisGenerationOperation.UPDATE_BY_ID)) {
			var updatableColumns = columns.stream().filter(column -> !column.primaryKey() && !column.autoIncrement()).toList();
			builder.append("\t<update id=\"update\" parameterType=\"").append(dtoType).append("\">\n");
			builder.append("\t\tUPDATE ").append(tableExpression).append("\n");
			builder.append("\t\tSET ");
			builder.append(updatableColumns.stream()
				.map(column -> column.columnName() + " = " + parameter(column))
				.collect(Collectors.joining(", ")));
			builder.append("\n");
			builder.append("\t\tWHERE ").append(whereClause(primaryKeys)).append("\n");
			builder.append("\t</update>\n\n");
		}
		if (effectiveOperations.contains(MyBatisGenerationOperation.DELETE_BY_ID)) {
			builder.append("\t<delete id=\"deleteById\">\n");
			builder.append("\t\tDELETE FROM ").append(tableExpression).append("\n");
			builder.append("\t\tWHERE ").append(whereClause(primaryKeys)).append("\n");
			builder.append("\t</delete>\n\n");
		}
		builder.append("</mapper>\n");
		return builder.toString();
	}

	private Set<MyBatisGenerationOperation> supportedOperations(
			Set<MyBatisGenerationOperation> requestedOperations,
			List<MyBatisColumnMapping> primaryKeys,
			List<MyBatisColumnMapping> columns,
			List<String> warnings) {
		var supported = requestedOperations == null || requestedOperations.isEmpty()
			? EnumSet.noneOf(MyBatisGenerationOperation.class)
			: EnumSet.copyOf(requestedOperations);
		if (primaryKeys.isEmpty()) {
			removeWithWarning(supported, MyBatisGenerationOperation.SELECT_BY_ID, warnings, "SELECT_BY_ID skipped because table has no primary key.");
			removeWithWarning(supported, MyBatisGenerationOperation.UPDATE_BY_ID, warnings, "UPDATE_BY_ID skipped because table has no primary key.");
			removeWithWarning(supported, MyBatisGenerationOperation.DELETE_BY_ID, warnings, "DELETE_BY_ID skipped because table has no primary key.");
		}
		if (columns.stream().allMatch(MyBatisColumnMapping::autoIncrement)) {
			removeWithWarning(supported, MyBatisGenerationOperation.INSERT, warnings, "INSERT skipped because all columns are auto-increment/generated.");
		}
		if (columns.stream().noneMatch(column -> !column.primaryKey() && !column.autoIncrement())) {
			removeWithWarning(supported, MyBatisGenerationOperation.UPDATE_BY_ID, warnings, "UPDATE_BY_ID skipped because table has no updatable non-primary-key columns.");
		}
		return supported;
	}

	private void removeWithWarning(
			Set<MyBatisGenerationOperation> operations,
			MyBatisGenerationOperation operation,
			List<String> warnings,
			String warning) {
		if (operations.remove(operation) && !warnings.contains(warning)) {
			warnings.add(warning);
		}
	}

	private boolean hasPrimaryKeyOperation(Set<MyBatisGenerationOperation> operations) {
		return operations.contains(MyBatisGenerationOperation.SELECT_BY_ID)
			|| operations.contains(MyBatisGenerationOperation.UPDATE_BY_ID)
			|| operations.contains(MyBatisGenerationOperation.DELETE_BY_ID);
	}

	private List<MyBatisColumnMapping> primaryKeys(List<MyBatisColumnMapping> columns) {
		return columns.stream()
			.filter(MyBatisColumnMapping::primaryKey)
			.sorted(Comparator.comparingInt(MyBatisColumnMapping::ordinalPosition))
			.toList();
	}

	private String primaryKeyParameters(List<MyBatisColumnMapping> primaryKeys) {
		if (primaryKeys.size() == 1) {
			var primaryKey = primaryKeys.getFirst();
			return primaryKey.javaType() + " " + primaryKey.propertyName();
		}
		return primaryKeys.stream()
			.map(column -> "@Param(\"" + column.propertyName() + "\") " + column.javaType() + " " + column.propertyName())
			.collect(Collectors.joining(", "));
	}

	private String whereClause(List<MyBatisColumnMapping> primaryKeys) {
		return primaryKeys.stream()
			.map(column -> column.columnName() + " = " + parameter(column))
			.collect(Collectors.joining(" AND "));
	}

	private java.util.Optional<MyBatisColumnMapping> singleGeneratedKey(List<MyBatisColumnMapping> columns) {
		var generatedKeys = columns.stream()
			.filter(column -> column.primaryKey() && column.autoIncrement())
			.toList();
		return generatedKeys.size() == 1 ? java.util.Optional.of(generatedKeys.getFirst()) : java.util.Optional.empty();
	}

	private String parameter(MyBatisColumnMapping column) {
		return "#{" + column.propertyName() + ",jdbcType=" + column.jdbcType() + "}";
	}

	private void addIdentifierWarnings(
			String schemaName,
			String tableName,
			List<MyBatisColumnMapping> columns,
			List<String> warnings) {
		if (!isSimpleSqlIdentifier(tableName) || (schemaName != null && !isSimpleSqlIdentifier(schemaName))) {
			warnings.add("Generated SQL does not quote identifiers; review table/schema names before using if they require quoting.");
		}
		if (columns.stream().anyMatch(column -> !isSimpleSqlIdentifier(column.columnName()))) {
			warnings.add("Generated SQL does not quote column identifiers; review column names before using if they require quoting.");
		}
	}

	private boolean isSimpleSqlIdentifier(String identifier) {
		return identifier != null && identifier.matches("[A-Za-z_][A-Za-z0-9_]*");
	}

	private String tableExpression(String schemaName, String tableName) {
		if (schemaName == null || schemaName.isBlank()) {
			return tableName;
		}
		return schemaName + "." + tableName;
	}

	private String toPath(String packageName, String fileName) {
		return "src/main/java/" + packageName.replace('.', '/') + "/" + fileName;
	}

	private JavaType resolveJavaType(DatabaseColumnMetadata column) {
		var typeName = column.jdbcTypeName().toLowerCase(Locale.ROOT);
		if ("uuid".equals(typeName)) {
			return new JavaType("UUID", "java.util.UUID");
		}
		if ("json".equals(typeName) || "jsonb".equals(typeName)) {
			return new JavaType("String", null);
		}
		return switch (column.jdbcTypeCode()) {
			case Types.BIGINT -> new JavaType("Long", null);
			case Types.INTEGER -> new JavaType("Integer", null);
			case Types.SMALLINT -> new JavaType("Short", null);
			case Types.TINYINT -> new JavaType("Byte", null);
			case Types.NUMERIC, Types.DECIMAL -> new JavaType("BigDecimal", "java.math.BigDecimal");
			case Types.REAL -> new JavaType("Float", null);
			case Types.FLOAT, Types.DOUBLE -> new JavaType("Double", null);
			case Types.BOOLEAN, Types.BIT -> new JavaType("Boolean", null);
			case Types.DATE -> new JavaType("LocalDate", "java.time.LocalDate");
			case Types.TIME, Types.TIME_WITH_TIMEZONE -> new JavaType("LocalTime", "java.time.LocalTime");
			case Types.TIMESTAMP -> new JavaType("LocalDateTime", "java.time.LocalDateTime");
			case Types.TIMESTAMP_WITH_TIMEZONE -> new JavaType("OffsetDateTime", "java.time.OffsetDateTime");
			case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> new JavaType("byte[]", null);
			default -> new JavaType("String", null);
		};
	}

	private String resolveJdbcType(DatabaseColumnMetadata column) {
		var typeName = column.jdbcTypeName().toLowerCase(Locale.ROOT);
		if ("uuid".equals(typeName) || "json".equals(typeName) || "jsonb".equals(typeName)) {
			return "OTHER";
		}
		return switch (column.jdbcTypeCode()) {
			case Types.CHAR, Types.NCHAR -> "CHAR";
			case Types.VARCHAR, Types.LONGVARCHAR, Types.NVARCHAR, Types.LONGNVARCHAR -> "VARCHAR";
			case Types.BIGINT -> "BIGINT";
			case Types.INTEGER -> "INTEGER";
			case Types.SMALLINT -> "SMALLINT";
			case Types.TINYINT -> "TINYINT";
			case Types.NUMERIC -> "NUMERIC";
			case Types.DECIMAL -> "DECIMAL";
			case Types.REAL -> "REAL";
			case Types.FLOAT -> "FLOAT";
			case Types.DOUBLE -> "DOUBLE";
			case Types.BOOLEAN, Types.BIT -> "BOOLEAN";
			case Types.DATE -> "DATE";
			case Types.TIME, Types.TIME_WITH_TIMEZONE -> "TIME";
			case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> "TIMESTAMP";
			case Types.BINARY -> "BINARY";
			case Types.VARBINARY, Types.LONGVARBINARY -> "VARBINARY";
			case Types.BLOB -> "BLOB";
			default -> "VARCHAR";
		};
	}

	private String toCamelCase(String value) {
		var pascal = toPascalCase(value);
		if (pascal.isEmpty()) {
			return "value";
		}
		return Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
	}

	private String toPascalCase(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		var words = value.replaceAll("([a-z])([A-Z])", "$1_$2").split("[^A-Za-z0-9]+");
		var builder = new StringBuilder();
		for (String word : words) {
			if (word.isBlank()) {
				continue;
			}
			var lower = word.toLowerCase(Locale.ROOT);
			builder.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
		}
		if (builder.isEmpty()) {
			return "Generated";
		}
		if (Character.isDigit(builder.charAt(0))) {
			builder.insert(0, "N");
		}
		return builder.toString();
	}

	private String firstNonBlank(String first, String second) {
		if (first != null && !first.isBlank()) {
			return first.trim();
		}
		return second == null || second.isBlank() ? null : second.trim();
	}

	private record JavaType(String simpleName, String importName) {
	}

}