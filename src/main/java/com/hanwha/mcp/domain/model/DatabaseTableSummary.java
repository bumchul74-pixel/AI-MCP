package com.hanwha.mcp.domain.model;

public record DatabaseTableSummary(
		String catalogName,
		String schemaName,
		String tableName,
		String tableType,
		String remarks) {

	public DatabaseTableSummary {
		if (tableName == null || tableName.isBlank()) {
			throw new IllegalArgumentException("tableName must not be blank");
		}
		tableName = tableName.trim();
		if (tableType == null || tableType.isBlank()) {
			tableType = "TABLE";
		}
	}

}