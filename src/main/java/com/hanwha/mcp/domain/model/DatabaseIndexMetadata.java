package com.hanwha.mcp.domain.model;

public record DatabaseIndexMetadata(
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

	public DatabaseIndexMetadata {
		if (indexName == null || indexName.isBlank()) {
			throw new IllegalArgumentException("indexName must not be blank");
		}
		if (columnName == null || columnName.isBlank()) {
			throw new IllegalArgumentException("columnName must not be blank");
		}
		indexName = indexName.trim();
		columnName = columnName.trim();
		if (indexType == null || indexType.isBlank()) {
			indexType = "OTHER";
		}
	}

}