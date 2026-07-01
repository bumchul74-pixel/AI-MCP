package com.hanwha.mcp.bootstrap.mcp;

import java.util.Set;

import com.hanwha.mcp.application.usecase.GenerateMyBatisMapperUseCase;
import com.hanwha.mcp.bootstrap.mcp.dto.DatabaseColumnMetadataToolResponse;
import com.hanwha.mcp.bootstrap.mcp.dto.DatabaseCommentToolResponse;
import com.hanwha.mcp.bootstrap.mcp.dto.DatabaseForeignKeyToolResponse;
import com.hanwha.mcp.bootstrap.mcp.dto.DatabaseIndexToolResponse;
import com.hanwha.mcp.bootstrap.mcp.dto.DatabaseSchemaToolRequest;
import com.hanwha.mcp.bootstrap.mcp.dto.DatabaseTableListToolResponse;
import com.hanwha.mcp.bootstrap.mcp.dto.DatabaseTableLookupToolRequest;
import com.hanwha.mcp.bootstrap.mcp.dto.DatabaseTableSearchToolRequest;
import com.hanwha.mcp.bootstrap.mcp.dto.DatabaseTableSearchToolResponse;
import com.hanwha.mcp.bootstrap.mcp.dto.GenerateMyBatisMapperToolRequest;
import com.hanwha.mcp.bootstrap.mcp.dto.GenerateMyBatisMapperToolResponse;
import com.hanwha.mcp.common.exception.InvalidMcpInputException;
import com.hanwha.mcp.domain.repository.DatabaseSchemaInspector;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class MyBatisGeneratorMcpAdapter {

	private static final Logger log = LoggerFactory.getLogger(MyBatisGeneratorMcpAdapter.class);
	private static final String GENERATE_MYBATIS_MAPPER_TOOL = "generate_mybatis_mapper";
	private static final String LIST_DATABASE_TABLES_TOOL = "list_database_tables";
	private static final String SEARCH_DATABASE_TABLES_TOOL = "search_database_tables";
	private static final String DESCRIBE_DATABASE_TABLE_COLUMNS_TOOL = "describe_database_table_columns";
	private static final String DESCRIBE_DATABASE_FOREIGN_KEYS_TOOL = "describe_database_foreign_keys";
	private static final String DESCRIBE_DATABASE_INDEXES_TOOL = "describe_database_indexes";
	private static final String DESCRIBE_DATABASE_COMMENTS_TOOL = "describe_database_comments";

	private final GenerateMyBatisMapperUseCase generateMyBatisMapperUseCase;
	private final DatabaseSchemaInspector databaseSchemaInspector;
	private final Validator validator;
	private final MeterRegistry meterRegistry;

	public MyBatisGeneratorMcpAdapter(
			GenerateMyBatisMapperUseCase generateMyBatisMapperUseCase,
			DatabaseSchemaInspector databaseSchemaInspector,
			Validator validator,
			MeterRegistry meterRegistry) {
		this.generateMyBatisMapperUseCase = generateMyBatisMapperUseCase;
		this.databaseSchemaInspector = databaseSchemaInspector;
		this.validator = validator;
		this.meterRegistry = meterRegistry;
	}

	@McpTool(
		name = GENERATE_MYBATIS_MAPPER_TOOL,
		title = "Generate MyBatis Mapper",
		description = "Inspects the configured local database table and generates MyBatis mapper SQL, mapper interface, and DTO source artifacts.",
		generateOutputSchema = true)
	public GenerateMyBatisMapperToolResponse generateMyBatisMapper(
			@McpToolParam(required = true, description = "Database table name. Use schemaName separately or pass schema.table.")
			String tableName,
			@McpToolParam(required = false, description = "Optional database schema name, for example public.")
			String schemaName,
			@McpToolParam(required = false, description = "Optional Java domain object simple name. Defaults to the table name in PascalCase.")
			String domainObjectName,
			@McpToolParam(required = false, description = "Optional base Java package. Defaults to mcp.mybatis.generator.base-package.")
			String basePackage,
			@McpToolParam(required = false, description = "Optional DTO Java package. Defaults to basePackage.dto.")
			String dtoPackage,
			@McpToolParam(required = false, description = "Optional mapper Java package. Defaults to basePackage.mapper.")
			String mapperPackage,
			@McpToolParam(required = false, description = "Optional operations: CRUD, SELECT, SELECT_BY_ID, SELECT_ALL, INSERT, UPDATE, UPDATE_BY_ID, DELETE, DELETE_BY_ID.")
			String operations) {
		return invokeTool(GENERATE_MYBATIS_MAPPER_TOOL, () -> {
			var request = validate(new GenerateMyBatisMapperToolRequest(
				tableName,
				schemaName,
				domainObjectName,
				basePackage,
				dtoPackage,
				mapperPackage,
				operations));
			return GenerateMyBatisMapperToolResponse.from(this.generateMyBatisMapperUseCase.generate(request.toQuery()));
		});
	}

	@McpTool(
		name = LIST_DATABASE_TABLES_TOOL,
		title = "List Database Tables",
		description = "Lists tables and views from the configured JDBC database. Optionally restricts by schema.",
		generateOutputSchema = true)
	public DatabaseTableListToolResponse listDatabaseTables(
			@McpToolParam(required = false, description = "Optional database schema name, for example public.")
			String schemaName) {
		return invokeTool(LIST_DATABASE_TABLES_TOOL, () -> {
			var request = validate(new DatabaseSchemaToolRequest(schemaName));
			return DatabaseTableListToolResponse.from(
				request.schemaName(),
				this.databaseSchemaInspector.listTables(request.schemaName()));
		});
	}

	@McpTool(
		name = SEARCH_DATABASE_TABLES_TOOL,
		title = "Search Database Tables",
		description = "Searches related tables by keyword across table names, schema names, table remarks, column names, column remarks, and column type names.",
		generateOutputSchema = true)
	public DatabaseTableSearchToolResponse searchDatabaseTables(
			@McpToolParam(required = true, description = "Keyword to search for in table metadata.")
			String keyword,
			@McpToolParam(required = false, description = "Optional database schema name, for example public.")
			String schemaName) {
		return invokeTool(SEARCH_DATABASE_TABLES_TOOL, () -> {
			var request = validate(new DatabaseTableSearchToolRequest(keyword, schemaName));
			return DatabaseTableSearchToolResponse.from(
				request.keyword(),
				request.schemaName(),
				this.databaseSchemaInspector.searchTables(request.schemaName(), request.keyword()));
		});
	}

	@McpTool(
		name = DESCRIBE_DATABASE_TABLE_COLUMNS_TOOL,
		title = "Describe Database Table Columns",
		description = "Returns column metadata for a database table, including JDBC type, primary-key flag, nullable flag, default value, and remarks.",
		generateOutputSchema = true)
	public DatabaseColumnMetadataToolResponse describeDatabaseTableColumns(
			@McpToolParam(required = true, description = "Database table name. Use schemaName separately or pass schema.table.")
			String tableName,
			@McpToolParam(required = false, description = "Optional database schema name, for example public.")
			String schemaName) {
		return invokeTool(DESCRIBE_DATABASE_TABLE_COLUMNS_TOOL, () -> {
			var request = validate(new DatabaseTableLookupToolRequest(tableName, schemaName));
			return DatabaseColumnMetadataToolResponse.from(
				this.databaseSchemaInspector.inspectTable(request.schemaName(), request.tableName()));
		});
	}

	@McpTool(
		name = DESCRIBE_DATABASE_FOREIGN_KEYS_TOOL,
		title = "Describe Database Foreign Keys",
		description = "Returns imported and exported foreign-key relationships for a database table.",
		generateOutputSchema = true)
	public DatabaseForeignKeyToolResponse describeDatabaseForeignKeys(
			@McpToolParam(required = true, description = "Database table name. Use schemaName separately or pass schema.table.")
			String tableName,
			@McpToolParam(required = false, description = "Optional database schema name, for example public.")
			String schemaName) {
		return invokeTool(DESCRIBE_DATABASE_FOREIGN_KEYS_TOOL, () -> {
			var request = validate(new DatabaseTableLookupToolRequest(tableName, schemaName));
			return DatabaseForeignKeyToolResponse.from(
				request.schemaName(),
				request.tableName(),
				this.databaseSchemaInspector.inspectForeignKeys(request.schemaName(), request.tableName()));
		});
	}

	@McpTool(
		name = DESCRIBE_DATABASE_INDEXES_TOOL,
		title = "Describe Database Indexes",
		description = "Returns index metadata for a database table, including uniqueness, indexed columns, ordinal position, type, sort direction, and filter condition.",
		generateOutputSchema = true)
	public DatabaseIndexToolResponse describeDatabaseIndexes(
			@McpToolParam(required = true, description = "Database table name. Use schemaName separately or pass schema.table.")
			String tableName,
			@McpToolParam(required = false, description = "Optional database schema name, for example public.")
			String schemaName) {
		return invokeTool(DESCRIBE_DATABASE_INDEXES_TOOL, () -> {
			var request = validate(new DatabaseTableLookupToolRequest(tableName, schemaName));
			return DatabaseIndexToolResponse.from(
				request.schemaName(),
				request.tableName(),
				this.databaseSchemaInspector.inspectIndexes(request.schemaName(), request.tableName()));
		});
	}

	@McpTool(
		name = DESCRIBE_DATABASE_COMMENTS_TOOL,
		title = "Describe Database Comments",
		description = "Returns table and column remarks/comments from the configured JDBC database metadata.",
		generateOutputSchema = true)
	public DatabaseCommentToolResponse describeDatabaseComments(
			@McpToolParam(required = true, description = "Database table name. Use schemaName separately or pass schema.table.")
			String tableName,
			@McpToolParam(required = false, description = "Optional database schema name, for example public.")
			String schemaName) {
		return invokeTool(DESCRIBE_DATABASE_COMMENTS_TOOL, () -> {
			var request = validate(new DatabaseTableLookupToolRequest(tableName, schemaName));
			return DatabaseCommentToolResponse.from(
				this.databaseSchemaInspector.inspectTable(request.schemaName(), request.tableName()));
		});
	}

	private <T> T invokeTool(String toolName, ToolInvocation<T> invocation) {
		var sample = Timer.start(this.meterRegistry);
		try {
			var response = invocation.invoke();
			this.meterRegistry.counter("mcp.tool.invocation.count", "tool", toolName).increment();
			log.info("mcp_tool_invoked tool={}", toolName);
			return response;
		}
		catch (IllegalArgumentException exception) {
			this.meterRegistry.counter("mcp.tool.failure.count", "tool", toolName).increment();
			log.warn("mcp_tool_failed tool={} reason={}", toolName, sanitize(exception.getMessage()));
			throw new InvalidMcpInputException(exception.getMessage());
		}
		catch (RuntimeException exception) {
			this.meterRegistry.counter("mcp.tool.failure.count", "tool", toolName).increment();
			log.warn("mcp_tool_failed tool={} reason={}", toolName, sanitize(exception.getMessage()));
			throw exception;
		}
		finally {
			sample.stop(Timer.builder("mcp.tool.latency")
				.tag("tool", toolName)
				.register(this.meterRegistry));
		}
	}

	private <T> T validate(T request) {
		Set<ConstraintViolation<T>> violations = this.validator.validate(request);
		if (!violations.isEmpty()) {
			throw InvalidMcpInputException.from(violations);
		}
		return request;
	}

	private String sanitize(String message) {
		if (message == null || message.isBlank()) {
			return "unknown";
		}
		return message.replaceAll("[\\r\\n\\t]", " ");
	}

	@FunctionalInterface
	private interface ToolInvocation<T> {

		T invoke();

	}

}