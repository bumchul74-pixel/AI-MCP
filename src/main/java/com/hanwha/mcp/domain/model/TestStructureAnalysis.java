package com.hanwha.mcp.domain.model;

import java.util.List;

public record TestStructureAnalysis(
		boolean present,
		List<String> directories,
		List<JavaPackageSummary> packages,
		long testSourceFileCount,
		long testClassCount,
		List<String> frameworks) {

	public TestStructureAnalysis {
		directories = List.copyOf(directories);
		packages = List.copyOf(packages);
		frameworks = List.copyOf(frameworks);
		if (testSourceFileCount < 0) {
			throw new IllegalArgumentException("testSourceFileCount must not be negative");
		}
		if (testClassCount < 0) {
			throw new IllegalArgumentException("testClassCount must not be negative");
		}
	}

	public static TestStructureAnalysis empty() {
		return new TestStructureAnalysis(false, List.of(), List.of(), 0, 0, List.of());
	}

}
