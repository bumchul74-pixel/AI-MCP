package com.hanwha.mcp.bootstrap.mcp.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DatabaseSchemaToolRequest(
		@Size(max = 128, message = "schemaName must be at most 128 characters")
		@Pattern(regexp = "^[^\\r\\n\\t\\u0000]*$", message = "schemaName contains unsupported characters")
		String schemaName) {

	public DatabaseSchemaToolRequest {
		schemaName = trim(schemaName);
	}

	private static String trim(String value) {
		if (value == null) {
			return null;
		}
		value = value.trim();
		return value.isBlank() ? null : value;
	}

}