package com.hanwha.mcp.bootstrap.mcp.dto;

import static com.hanwha.mcp.bootstrap.mcp.dto.DatabaseMetadataResponseSupport.text;

import java.util.List;

import com.hanwha.mcp.domain.model.DatabaseIndexMetadata;

public record DatabaseIndexToolResponse(
		String schemaName,
		String tableName,
		int indexColumnCount,
		List<IndexResponse> indexes) {

	public static DatabaseIndexToolResponse from(
			String schemaName,
			String tableName,
			List<DatabaseIndexMetadata> indexes) {
		return new DatabaseIndexToolResponse(
			text(schemaName),
			text(tableName),
			indexes.size(),
			indexes.stream().map(IndexResponse::from).toList());
	}

	public record IndexResponse(
			String indexQualifier,
			String indexName,
			String columnName,
			int ordinalPosition,
			boolean unique,
			String indexType,
			String sortDirection,
			Long cardinality,
			Long pages,
			String filterCondition) {

		static IndexResponse from(DatabaseIndexMetadata metadata) {
			return new IndexResponse(
				text(metadata.indexQualifier()),
				text(metadata.indexName()),
				text(metadata.columnName()),
				metadata.ordinalPosition(),
				metadata.unique(),
				text(metadata.indexType()),
				text(metadata.sortDirection()),
				metadata.cardinality(),
				metadata.pages(),
				text(metadata.filterCondition()));
		}

	}

}