package com.hanwha.mcp.bootstrap.mcp.dto;

import static com.hanwha.mcp.bootstrap.mcp.dto.DatabaseMetadataResponseSupport.text;

import com.hanwha.mcp.domain.model.DatabaseTableSummary;

public record DatabaseTableSummaryResponse(
		String catalogName,
		String schemaName,
		String tableName,
		String tableType,
		String remarks) {

	public static DatabaseTableSummaryResponse from(DatabaseTableSummary table) {
		return new DatabaseTableSummaryResponse(
			text(table.catalogName()),
			text(table.schemaName()),
			text(table.tableName()),
			text(table.tableType()),
			text(table.remarks()));
	}

}