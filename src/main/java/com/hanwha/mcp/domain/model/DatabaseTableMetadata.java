package com.hanwha.mcp.domain.model;

import java.util.Comparator;
import java.util.List;

public record DatabaseTableMetadata(
		String catalogName,
		String schemaName,
		String tableName,
		String tableType,
		String remarks,
		String databaseProductName,
		String databaseProductVersion,
		List<DatabaseColumnMetadata> columns) {

	public DatabaseTableMetadata {
		if (tableName == null || tableName.isBlank()) {
			throw new IllegalArgumentException("tableName must not be blank");
		}
		tableName = tableName.trim();
		if (tableType == null || tableType.isBlank()) {
			tableType = "TABLE";
		}
		if (databaseProductName == null || databaseProductName.isBlank()) {
			databaseProductName = "unknown";
		}
		if (databaseProductVersion == null || databaseProductVersion.isBlank()) {
			databaseProductVersion = "unknown";
		}
		columns = columns.stream()
			.sorted(Comparator.comparingInt(DatabaseColumnMetadata::ordinalPosition))
			.toList();
		if (columns.isEmpty()) {
			throw new IllegalArgumentException("columns must not be empty");
		}
	}

	public List<DatabaseColumnMetadata> primaryKeyColumns() {
		return this.columns.stream().filter(DatabaseColumnMetadata::primaryKey).toList();
	}

}