package com.hanwha.mcp.bootstrap.mcp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DatabaseTableLookupToolRequest(
		@NotBlank(message = "tableName must not be blank")
		@Size(max = 256, message = "tableName must be at most 256 characters")
		@Pattern(regexp = "^[^\\r\\n\\t\\u0000]+$", message = "tableName contains unsupported characters")
		String tableName,

		@Size(max = 128, message = "schemaName must be at most 128 characters")
		@Pattern(regexp = "^[^\\r\\n\\t\\u0000]*$", message = "schemaName contains unsupported characters")
		String schemaName) {

	public DatabaseTableLookupToolRequest {
		tableName = trim(tableName);
		schemaName = trim(schemaName);
		if ((schemaName == null || schemaName.isBlank()) && tableName != null && tableName.indexOf('.') > 0) {
			var parts = tableName.split("\\.", 2);
			schemaName = trim(parts[0]);
			tableName = trim(parts[1]);
		}
	}

	private static String trim(String value) {
		if (value == null) {
			return null;
		}
		value = value.trim();
		return value.isBlank() ? null : value;
	}

}