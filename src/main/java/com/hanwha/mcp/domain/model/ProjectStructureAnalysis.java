package com.hanwha.mcp.domain.model;

import java.util.List;

public record ProjectStructureAnalysis(
		String projectPath,
		String projectName,
		List<JavaPackageSummary> packages,
		GradleBuildAnalysis build,
		TestStructureAnalysis testStructure,
		List<String> warnings) {

	public ProjectStructureAnalysis {
		validateText(projectPath, "projectPath");
		validateText(projectName, "projectName");
		packages = List.copyOf(packages);
		if (build == null) {
			build = GradleBuildAnalysis.missing();
		}
		if (testStructure == null) {
			testStructure = TestStructureAnalysis.empty();
		}
		warnings = List.copyOf(warnings);
	}

	private static void validateText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
	}

}
