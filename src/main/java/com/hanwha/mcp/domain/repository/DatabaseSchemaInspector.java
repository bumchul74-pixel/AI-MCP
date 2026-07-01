package com.hanwha.mcp.domain.repository;

import java.util.List;

import com.hanwha.mcp.domain.model.DatabaseForeignKeyMetadata;
import com.hanwha.mcp.domain.model.DatabaseIndexMetadata;
import com.hanwha.mcp.domain.model.DatabaseTableMetadata;
import com.hanwha.mcp.domain.model.DatabaseTableSummary;

public interface DatabaseSchemaInspector {

	DatabaseTableMetadata inspectTable(String schemaName, String tableName);

	default List<DatabaseTableSummary> listTables(String schemaName) {
		throw new UnsupportedOperationException("listTables is not supported");
	}

	default List<DatabaseTableSummary> searchTables(String schemaName, String keyword) {
		throw new UnsupportedOperationException("searchTables is not supported");
	}

	default List<DatabaseForeignKeyMetadata> inspectForeignKeys(String schemaName, String tableName) {
		throw new UnsupportedOperationException("inspectForeignKeys is not supported");
	}

	default List<DatabaseIndexMetadata> inspectIndexes(String schemaName, String tableName) {
		throw new UnsupportedOperationException("inspectIndexes is not supported");
	}

}