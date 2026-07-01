package com.hanwha.mcp.domain.model;

public record GeneratedFileArtifact(
		String role,
		String fileName,
		String suggestedPath,
		String language,
		String content) {

	public GeneratedFileArtifact {
		validateText(role, "role");
		validateText(fileName, "fileName");
		validateText(suggestedPath, "suggestedPath");
		validateText(language, "language");
		validateText(content, "content");
	}

	private static void validateText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
	}

}