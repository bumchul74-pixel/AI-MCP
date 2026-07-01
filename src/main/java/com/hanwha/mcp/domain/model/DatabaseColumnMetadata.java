package com.hanwha.mcp.domain.model;

public record DatabaseColumnMetadata(
		String name,
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

	public DatabaseColumnMetadata {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("column name must not be blank");
		}
		name = name.trim();
		if (jdbcTypeName == null || jdbcTypeName.isBlank()) {
			jdbcTypeName = "UNKNOWN";
		}
		else {
			jdbcTypeName = jdbcTypeName.trim();
		}
		if (ordinalPosition < 0) {
			ordinalPosition = 0;
		}
	}

}