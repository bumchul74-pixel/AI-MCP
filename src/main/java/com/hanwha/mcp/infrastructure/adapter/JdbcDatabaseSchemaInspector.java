package com.hanwha.mcp.infrastructure.adapter;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import com.hanwha.mcp.bootstrap.config.MyBatisDataSourceProperties;
import com.hanwha.mcp.domain.model.DatabaseColumnMetadata;
import com.hanwha.mcp.domain.model.DatabaseForeignKeyMetadata;
import com.hanwha.mcp.domain.model.DatabaseIndexMetadata;
import com.hanwha.mcp.domain.model.DatabaseTableMetadata;
import com.hanwha.mcp.domain.model.DatabaseTableSummary;
import com.hanwha.mcp.domain.repository.DatabaseSchemaInspector;

public class JdbcDatabaseSchemaInspector implements DatabaseSchemaInspector {

	private static final String[] TABLE_TYPES = { "TABLE", "VIEW" };

	private final MyBatisDataSourceProperties properties;

	public JdbcDatabaseSchemaInspector(MyBatisDataSourceProperties properties) {
		this.properties = properties;
	}

	@Override
	public DatabaseTableMetadata inspectTable(String schemaName, String tableName) {
		try (Connection connection = openConnection()) {
			var metadata = connection.getMetaData();
			var table = findTable(metadata, connection.getCatalog(), schemaName, tableName);
			var primaryKeys = primaryKeys(metadata, table.catalogName(), table.schemaName(), table.tableName());
			var columns = columns(metadata, table.catalogName(), table.schemaName(), table.tableName(), primaryKeys);
			return new DatabaseTableMetadata(
				table.catalogName(),
				table.schemaName(),
				table.tableName(),
				table.tableType(),
				table.remarks(),
				metadata.getDatabaseProductName(),
				metadata.getDatabaseProductVersion(),
				columns);
		}
		catch (SQLException exception) {
			throw new IllegalStateException("Unable to inspect database table metadata: " + exception.getMessage(), exception);
		}
	}

	@Override
	public List<DatabaseTableSummary> listTables(String schemaName) {
		try (Connection connection = openConnection()) {
			return findTables(connection.getMetaData(), connection.getCatalog(), schemaName).stream()
				.map(TableRow::toSummary)
				.toList();
		}
		catch (SQLException exception) {
			throw new IllegalStateException("Unable to list database tables: " + exception.getMessage(), exception);
		}
	}

	@Override
	public List<DatabaseTableSummary> searchTables(String schemaName, String keyword) {
		if (keyword == null || keyword.isBlank()) {
			throw new IllegalArgumentException("keyword must not be blank");
		}
		var normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
		try (Connection connection = openConnection()) {
			var metadata = connection.getMetaData();
			return findTables(metadata, connection.getCatalog(), schemaName).stream()
				.filter(table -> matchesKeyword(table.schemaName(), normalizedKeyword)
					|| matchesKeyword(table.tableName(), normalizedKeyword)
					|| matchesKeyword(table.remarks(), normalizedKeyword)
					|| hasMatchingColumn(metadata, table, normalizedKeyword))
				.map(TableRow::toSummary)
				.toList();
		}
		catch (SQLException exception) {
			throw new IllegalStateException("Unable to search database tables: " + exception.getMessage(), exception);
		}
	}

	@Override
	public List<DatabaseForeignKeyMetadata> inspectForeignKeys(String schemaName, String tableName) {
		try (Connection connection = openConnection()) {
			var metadata = connection.getMetaData();
			var table = findTable(metadata, connection.getCatalog(), schemaName, tableName);
			var foreignKeys = new ArrayList<DatabaseForeignKeyMetadata>();
			foreignKeys.addAll(foreignKeys(metadata, table, ForeignKeyDirection.IMPORTED));
			foreignKeys.addAll(foreignKeys(metadata, table, ForeignKeyDirection.EXPORTED));
			return foreignKeys.stream()
				.sorted(Comparator.comparing(DatabaseForeignKeyMetadata::direction)
					.thenComparing(metadataRow -> nullToEmpty(metadataRow.foreignKeyName()))
					.thenComparingInt(DatabaseForeignKeyMetadata::keySequence))
				.toList();
		}
		catch (SQLException exception) {
			throw new IllegalStateException("Unable to inspect database foreign keys: " + exception.getMessage(), exception);
		}
	}

	@Override
	public List<DatabaseIndexMetadata> inspectIndexes(String schemaName, String tableName) {
		try (Connection connection = openConnection()) {
			var metadata = connection.getMetaData();
			var table = findTable(metadata, connection.getCatalog(), schemaName, tableName);
			var indexes = new ArrayList<DatabaseIndexMetadata>();
			try (ResultSet resultSet = metadata.getIndexInfo(table.catalogName(), table.schemaName(), table.tableName(), false, false)) {
				while (resultSet.next()) {
					if (DatabaseMetaData.tableIndexStatistic == resultSet.getShort("TYPE")) {
						continue;
					}
					var indexName = getString(resultSet, "INDEX_NAME");
					var columnName = getString(resultSet, "COLUMN_NAME");
					if (indexName == null || indexName.isBlank() || columnName == null || columnName.isBlank()) {
						continue;
					}
					indexes.add(new DatabaseIndexMetadata(
						getString(resultSet, "INDEX_QUALIFIER"),
						indexName,
						columnName,
						resultSet.getInt("ORDINAL_POSITION"),
						!resultSet.getBoolean("NON_UNIQUE"),
						indexTypeName(resultSet.getShort("TYPE")),
						sortDirectionName(getString(resultSet, "ASC_OR_DESC")),
						getNullableLong(resultSet, "CARDINALITY"),
						getNullableLong(resultSet, "PAGES"),
						getString(resultSet, "FILTER_CONDITION")));
				}
			}
			return indexes.stream()
				.sorted(Comparator.comparing(DatabaseIndexMetadata::indexName)
					.thenComparingInt(DatabaseIndexMetadata::ordinalPosition))
				.toList();
		}
		catch (SQLException exception) {
			throw new IllegalStateException("Unable to inspect database indexes: " + exception.getMessage(), exception);
		}
	}

	private Connection openConnection() throws SQLException {
		if (this.properties.url() == null) {
			throw new IllegalStateException("mcp.mybatis.datasource.url must be configured before using MyBatis database metadata tools.");
		}
		loadDriver();
		return DriverManager.getConnection(this.properties.url(), connectionProperties());
	}

	private void loadDriver() {
		if (this.properties.driverClassName() == null) {
			return;
		}
		try {
			Class.forName(this.properties.driverClassName());
		}
		catch (ClassNotFoundException exception) {
			throw new IllegalStateException("Configured JDBC driver class was not found: " + this.properties.driverClassName(), exception);
		}
	}

	private Properties connectionProperties() {
		var connectionProperties = new Properties();
		if (this.properties.username() != null) {
			connectionProperties.put("user", this.properties.username());
		}
		if (this.properties.password() != null) {
			connectionProperties.put("password", this.properties.password());
		}
		return connectionProperties;
	}

	private TableRow findTable(DatabaseMetaData metadata, String catalogName, String schemaName, String tableName)
			throws SQLException {
		if (tableName == null || tableName.isBlank()) {
			throw new IllegalArgumentException("tableName must not be blank");
		}
		var catalogs = orderedCandidates(catalogName, null);
		var schemas = schemaCandidates(schemaName);
		var tablePatterns = orderedCandidates(tableName, tableName.toLowerCase(Locale.ROOT), tableName.toUpperCase(Locale.ROOT), "%");
		for (String catalog : catalogs) {
			for (String schema : schemas) {
				for (String tablePattern : tablePatterns) {
					try (ResultSet resultSet = metadata.getTables(catalog, schema, tablePattern, TABLE_TYPES)) {
						while (resultSet.next()) {
							var candidate = tableRow(resultSet);
							if (matches(schemaName, candidate.schemaName()) && candidate.tableName().equalsIgnoreCase(tableName)) {
								return candidate;
							}
						}
					}
				}
			}
		}
		throw new IllegalArgumentException("Table not found in configured database: " + qualifiedName(schemaName, tableName));
	}

	private List<TableRow> findTables(DatabaseMetaData metadata, String catalogName, String schemaName) throws SQLException {
		var tables = new LinkedHashMap<String, TableRow>();
		for (String catalog : orderedCandidates(catalogName, null)) {
			for (String schema : schemaCandidates(schemaName)) {
				try (ResultSet resultSet = metadata.getTables(catalog, schema, "%", TABLE_TYPES)) {
					while (resultSet.next()) {
						var table = tableRow(resultSet);
						if (matches(schemaName, table.schemaName())) {
							tables.putIfAbsent(table.identityKey(), table);
						}
					}
				}
			}
		}
		return tables.values().stream()
			.sorted(Comparator.comparing((TableRow table) -> nullToEmpty(table.schemaName()))
				.thenComparing(TableRow::tableName))
			.toList();
	}

	private TableRow tableRow(ResultSet resultSet) throws SQLException {
		return new TableRow(
			resultSet.getString("TABLE_CAT"),
			resultSet.getString("TABLE_SCHEM"),
			resultSet.getString("TABLE_NAME"),
			resultSet.getString("TABLE_TYPE"),
			resultSet.getString("REMARKS"));
	}

	private boolean hasMatchingColumn(DatabaseMetaData metadata, TableRow table, String keyword) {
		try (ResultSet resultSet = metadata.getColumns(table.catalogName(), table.schemaName(), table.tableName(), null)) {
			while (resultSet.next()) {
				if (matchesKeyword(getString(resultSet, "COLUMN_NAME"), keyword)
						|| matchesKeyword(getString(resultSet, "TYPE_NAME"), keyword)
						|| matchesKeyword(getString(resultSet, "REMARKS"), keyword)) {
					return true;
				}
			}
			return false;
		}
		catch (SQLException exception) {
			return false;
		}
	}

	private boolean matchesKeyword(String value, String keyword) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
	}

	private Set<String> primaryKeys(DatabaseMetaData metadata, String catalogName, String schemaName, String tableName)
			throws SQLException {
		var primaryKeys = new HashSet<String>();
		try (ResultSet resultSet = metadata.getPrimaryKeys(catalogName, schemaName, tableName)) {
			while (resultSet.next()) {
				primaryKeys.add(resultSet.getString("COLUMN_NAME"));
			}
		}
		return primaryKeys;
	}

	private List<DatabaseColumnMetadata> columns(
			DatabaseMetaData metadata,
			String catalogName,
			String schemaName,
			String tableName,
			Set<String> primaryKeys)
			throws SQLException {
		var columns = new ArrayList<DatabaseColumnMetadata>();
		try (ResultSet resultSet = metadata.getColumns(catalogName, schemaName, tableName, null)) {
			while (resultSet.next()) {
				var columnName = resultSet.getString("COLUMN_NAME");
				columns.add(new DatabaseColumnMetadata(
					columnName,
					resultSet.getInt("DATA_TYPE"),
					resultSet.getString("TYPE_NAME"),
					DatabaseMetaData.columnNullable == resultSet.getInt("NULLABLE"),
					primaryKeys.contains(columnName),
					"YES".equalsIgnoreCase(getString(resultSet, "IS_AUTOINCREMENT"))
						|| "YES".equalsIgnoreCase(getString(resultSet, "IS_GENERATEDCOLUMN")),
					resultSet.getInt("ORDINAL_POSITION"),
					getNullableInt(resultSet, "COLUMN_SIZE"),
					getNullableInt(resultSet, "DECIMAL_DIGITS"),
					getString(resultSet, "COLUMN_DEF"),
					getString(resultSet, "REMARKS")));
			}
		}
		if (columns.isEmpty()) {
			throw new IllegalArgumentException("No columns found for table: " + qualifiedName(schemaName, tableName));
		}
		return columns;
	}

	private List<DatabaseForeignKeyMetadata> foreignKeys(
			DatabaseMetaData metadata,
			TableRow table,
			ForeignKeyDirection direction)
			throws SQLException {
		var foreignKeys = new ArrayList<DatabaseForeignKeyMetadata>();
		try (ResultSet resultSet = direction.resultSet(metadata, table)) {
			while (resultSet.next()) {
				foreignKeys.add(new DatabaseForeignKeyMetadata(
					direction.name(),
					getString(resultSet, "FK_NAME"),
					getString(resultSet, "PK_NAME"),
					getString(resultSet, "PKTABLE_CAT"),
					getString(resultSet, "PKTABLE_SCHEM"),
					getString(resultSet, "PKTABLE_NAME"),
					getString(resultSet, "PKCOLUMN_NAME"),
					getString(resultSet, "FKTABLE_CAT"),
					getString(resultSet, "FKTABLE_SCHEM"),
					getString(resultSet, "FKTABLE_NAME"),
					getString(resultSet, "FKCOLUMN_NAME"),
					resultSet.getInt("KEY_SEQ"),
					foreignKeyRuleName(resultSet.getShort("UPDATE_RULE")),
					foreignKeyRuleName(resultSet.getShort("DELETE_RULE")),
					deferrabilityName(resultSet.getShort("DEFERRABILITY"))));
			}
		}
		return foreignKeys;
	}

	private Integer getNullableInt(ResultSet resultSet, String columnName) throws SQLException {
		var value = resultSet.getInt(columnName);
		return resultSet.wasNull() ? null : value;
	}

	private Long getNullableLong(ResultSet resultSet, String columnName) throws SQLException {
		var value = resultSet.getLong(columnName);
		return resultSet.wasNull() ? null : value;
	}

	private String getString(ResultSet resultSet, String columnName) {
		try {
			return resultSet.getString(columnName);
		}
		catch (SQLException exception) {
			return null;
		}
	}

	private List<String> schemaCandidates(String schemaName) {
		if (schemaName == null || schemaName.isBlank()) {
			return orderedCandidates((String) null);
		}
		return orderedCandidates(schemaName, schemaName.toLowerCase(Locale.ROOT), schemaName.toUpperCase(Locale.ROOT), null);
	}

	private List<String> orderedCandidates(String... values) {
		var ordered = new LinkedHashSet<String>();
		for (String value : values) {
			if (value == null || value.isBlank()) {
				ordered.add(null);
			}
			else {
				ordered.add(value);
			}
		}
		return new ArrayList<>(ordered);
	}

	private boolean matches(String requestedSchema, String actualSchema) {
		return requestedSchema == null || requestedSchema.isBlank() || requestedSchema.equalsIgnoreCase(actualSchema);
	}

	private String qualifiedName(String schemaName, String tableName) {
		if (schemaName == null || schemaName.isBlank()) {
			return tableName;
		}
		return schemaName + "." + tableName;
	}

	private String indexTypeName(short type) {
		return switch (type) {
			case DatabaseMetaData.tableIndexClustered -> "CLUSTERED";
			case DatabaseMetaData.tableIndexHashed -> "HASHED";
			case DatabaseMetaData.tableIndexOther -> "OTHER";
			default -> "UNKNOWN";
		};
	}

	private String sortDirectionName(String value) {
		if ("A".equalsIgnoreCase(value)) {
			return "ASC";
		}
		if ("D".equalsIgnoreCase(value)) {
			return "DESC";
		}
		return value;
	}

	private String foreignKeyRuleName(short rule) {
		return switch (rule) {
			case DatabaseMetaData.importedKeyCascade -> "CASCADE";
			case DatabaseMetaData.importedKeyRestrict -> "RESTRICT";
			case DatabaseMetaData.importedKeySetNull -> "SET_NULL";
			case DatabaseMetaData.importedKeyNoAction -> "NO_ACTION";
			case DatabaseMetaData.importedKeySetDefault -> "SET_DEFAULT";
			default -> "UNKNOWN";
		};
	}

	private String deferrabilityName(short deferrability) {
		return switch (deferrability) {
			case DatabaseMetaData.importedKeyInitiallyDeferred -> "INITIALLY_DEFERRED";
			case DatabaseMetaData.importedKeyInitiallyImmediate -> "INITIALLY_IMMEDIATE";
			case DatabaseMetaData.importedKeyNotDeferrable -> "NOT_DEFERRABLE";
			default -> "UNKNOWN";
		};
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private record TableRow(
			String catalogName,
			String schemaName,
			String tableName,
			String tableType,
			String remarks) {

		String identityKey() {
			return nullToEmpty(this.catalogName) + "." + nullToEmpty(this.schemaName) + "." + this.tableName;
		}

		DatabaseTableSummary toSummary() {
			return new DatabaseTableSummary(this.catalogName, this.schemaName, this.tableName, this.tableType, this.remarks);
		}

		private static String nullToEmpty(String value) {
			return value == null ? "" : value;
		}

	}

	private enum ForeignKeyDirection {

		IMPORTED {
			@Override
			ResultSet resultSet(DatabaseMetaData metadata, TableRow table) throws SQLException {
				return metadata.getImportedKeys(table.catalogName(), table.schemaName(), table.tableName());
			}
		},
		EXPORTED {
			@Override
			ResultSet resultSet(DatabaseMetaData metadata, TableRow table) throws SQLException {
				return metadata.getExportedKeys(table.catalogName(), table.schemaName(), table.tableName());
			}
		};

		abstract ResultSet resultSet(DatabaseMetaData metadata, TableRow table) throws SQLException;

	}

}