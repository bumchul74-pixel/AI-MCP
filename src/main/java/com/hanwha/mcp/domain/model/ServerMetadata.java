package com.hanwha.mcp.domain.model;

public record ServerMetadata(String name, String version, String description) {

	public ServerMetadata {
		validateText(name, "name");
		validateText(version, "version");
		validateText(description, "description");
	}

	private static void validateText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
	}

}