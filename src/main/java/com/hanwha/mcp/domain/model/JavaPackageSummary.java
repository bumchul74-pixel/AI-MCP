package com.hanwha.mcp.domain.model;

public record JavaPackageSummary(String sourceSet, String packageName, long sourceFileCount) {

	public JavaPackageSummary {
		validateText(sourceSet, "sourceSet");
		validateText(packageName, "packageName");
		if (sourceFileCount < 0) {
			throw new IllegalArgumentException("sourceFileCount must not be negative");
		}
	}

	private static void validateText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
	}

}
