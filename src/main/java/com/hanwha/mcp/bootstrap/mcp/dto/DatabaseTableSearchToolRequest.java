package com.hanwha.mcp.bootstrap.mcp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DatabaseTableSearchToolRequest(
		@NotBlank(message = "keyword must not be blank")
		@Size(max = 128, message = "keyword must be at most 128 characters")
		@Pattern(regexp = "^[^\\r\\n\\t\\u0000]+$", message = "keyword contains unsupported characters")
		String keyword,

		@Size(max = 128, message = "schemaName must be at most 128 characters")
		@Pattern(regexp = "^[^\\r\\n\\t\\u0000]*$", message = "schemaName contains unsupported characters")
		String schemaName) {

	public DatabaseTableSearchToolRequest {
		keyword = trim(keyword);
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