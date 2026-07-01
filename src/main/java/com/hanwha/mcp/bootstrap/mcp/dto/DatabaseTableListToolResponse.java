package com.hanwha.mcp.bootstrap.mcp.dto;

import static com.hanwha.mcp.bootstrap.mcp.dto.DatabaseMetadataResponseSupport.text;

import java.util.List;

import com.hanwha.mcp.domain.model.DatabaseTableSummary;

public record DatabaseTableListToolResponse(
		String schemaName,
		int tableCount,
		List<DatabaseTableSummaryResponse> tables) {

	public static DatabaseTableListToolResponse from(String schemaName, List<DatabaseTableSummary> tables) {
		return new DatabaseTableListToolResponse(
			text(schemaName),
			tables.size(),
			tables.stream().map(DatabaseTableSummaryResponse::from).toList());
	}

}