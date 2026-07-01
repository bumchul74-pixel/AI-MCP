package com.hanwha.mcp.bootstrap.mcp.dto;

import static com.hanwha.mcp.bootstrap.mcp.dto.DatabaseMetadataResponseSupport.text;

import java.util.List;

import com.hanwha.mcp.domain.model.DatabaseColumnMetadata;
import com.hanwha.mcp.domain.model.DatabaseTableMetadata;

public record DatabaseColumnMetadataToolResponse(
		String databaseProductName,
		String databaseProductVersion,
		String catalogName,
		String schemaName,
		String tableName,
		String tableType,
		String tableRemarks,
		List<String> primaryKeyColumns,
		List<ColumnResponse> columns) {

	public static DatabaseColumnMetadataToolResponse from(DatabaseTableMetadata table) {
		return new DatabaseColumnMetadataToolResponse(
			text(table.databaseProductName()),
			text(table.databaseProductVersion()),
			text(table.catalogName()),
			text(table.schemaName()),
			text(table.tableName()),
			text(table.tableType()),
			text(table.remarks()),
			table.primaryKeyColumns().stream().map(DatabaseColumnMetadata::name).toList(),
			table.columns().stream().map(ColumnResponse::from).toList());
	}

	public record ColumnResponse(
			String columnName,
			int jdbcTypeCode,
			String jdbcTypeName,
			boolean nullable,
			boolean primaryKey,
			boolean autoIncrement,
			int ordinalPosition,
			Integer size,
			Integer decimalDigits,
			String defaultValue,
			String remarks) {

		static ColumnResponse from(DatabaseColumnMetadata column) {
			return new ColumnResponse(
				text(column.name()),
				column.jdbcTypeCode(),
				text(column.jdbcTypeName()),
				column.nullable(),
				column.primaryKey(),
				column.autoIncrement(),
				column.ordinalPosition(),
				column.size(),
				column.decimalDigits(),
				text(column.defaultValue()),
				text(column.remarks()));
		}

	}

}