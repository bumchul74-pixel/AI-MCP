package com.hanwha.mcp.domain.model;

import java.util.List;

public record GradleBuildAnalysis(
		boolean present,
		String fileName,
		String relativePath,
		boolean kotlinDsl,
		String javaVersion,
		String springBootVersion,
		List<GradleDependency> dependencies) {

	public GradleBuildAnalysis {
		dependencies = List.copyOf(dependencies);
	}

	public static GradleBuildAnalysis missing() {
		return new GradleBuildAnalysis(false, null, null, false, null, null, List.of());
	}

}
