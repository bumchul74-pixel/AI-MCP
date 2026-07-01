package com.hanwha.mcp.bootstrap.mcp.dto;

import static com.hanwha.mcp.bootstrap.mcp.dto.DatabaseMetadataResponseSupport.text;

import java.util.List;

import com.hanwha.mcp.domain.model.DatabaseTableSummary;

public record DatabaseTableSearchToolResponse(
		String keyword,
		String schemaName,
		int tableCount,
		List<DatabaseTableSummaryResponse> tables) {

	public static DatabaseTableSearchToolResponse from(String keyword, String schemaName, List<DatabaseTableSummary> tables) {
		return new DatabaseTableSearchToolResponse(
			text(keyword),
			text(schemaName),
			tables.size(),
			tables.stream().map(DatabaseTableSummaryResponse::from).toList());
	}

}