package com.hanwha.mcp.domain.model;

public record DatabaseForeignKeyMetadata(
		String direction,
		String foreignKeyName,
		String primaryKeyName,
		String primaryKeyCatalogName,
		String primaryKeySchemaName,
		String primaryKeyTableName,
		String primaryKeyColumnName,
		String foreignKeyCatalogName,
		String foreignKeySchemaName,
		String foreignKeyTableName,
		String foreignKeyColumnName,
		int keySequence,
		String updateRule,
		String deleteRule,
		String deferrability) {

	public DatabaseForeignKeyMetadata {
		validateText(direction, "direction");
		validateText(primaryKeyTableName, "primaryKeyTableName");
		validateText(primaryKeyColumnName, "primaryKeyColumnName");
		validateText(foreignKeyTableName, "foreignKeyTableName");
		validateText(foreignKeyColumnName, "foreignKeyColumnName");
	}

	private static void validateText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
	}

}