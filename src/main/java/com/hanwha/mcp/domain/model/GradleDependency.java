package com.hanwha.mcp.domain.model;

public record GradleDependency(String configuration, String notation) {

	public GradleDependency {
		validateText(configuration, "configuration");
		validateText(notation, "notation");
	}

	private static void validateText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
	}

}
