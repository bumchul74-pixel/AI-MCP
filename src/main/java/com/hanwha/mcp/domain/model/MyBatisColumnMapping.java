package com.hanwha.mcp.domain.model;

public record MyBatisColumnMapping(
		String columnName,
		String propertyName,
		String jdbcType,
		String javaType,
		String javaImport,
		boolean nullable,
		boolean primaryKey,
		boolean autoIncrement,
		int ordinalPosition) {

	public MyBatisColumnMapping {
		validateText(columnName, "columnName");
		validateText(propertyName, "propertyName");
		validateText(jdbcType, "jdbcType");
		validateText(javaType, "javaType");
	}

	private static void validateText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
	}

}