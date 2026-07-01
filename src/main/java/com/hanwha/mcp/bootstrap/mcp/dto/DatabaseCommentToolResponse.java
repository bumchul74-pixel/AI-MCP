package com.hanwha.mcp.bootstrap.mcp.dto;

import static com.hanwha.mcp.bootstrap.mcp.dto.DatabaseMetadataResponseSupport.text;

import java.util.List;

import com.hanwha.mcp.domain.model.DatabaseColumnMetadata;
import com.hanwha.mcp.domain.model.DatabaseTableMetadata;

public record DatabaseCommentToolResponse(
		String catalogName,
		String schemaName,
		String tableName,
		String tableType,
		String tableRemarks,
		List<ColumnCommentResponse> columns) {

	public static DatabaseCommentToolResponse from(DatabaseTableMetadata table) {
		return new DatabaseCommentToolResponse(
			text(table.catalogName()),
			text(table.schemaName()),
			text(table.tableName()),
			text(table.tableType()),
			text(table.remarks()),
			table.columns().stream().map(ColumnCommentResponse::from).toList());
	}

	public record ColumnCommentResponse(
			String columnName,
			String remarks) {

		static ColumnCommentResponse from(DatabaseColumnMetadata column) {
			return new ColumnCommentResponse(text(column.name()), text(column.remarks()));
		}

	}

}