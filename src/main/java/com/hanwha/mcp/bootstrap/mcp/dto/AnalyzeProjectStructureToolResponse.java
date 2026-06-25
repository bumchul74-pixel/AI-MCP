package com.hanwha.mcp.bootstrap.mcp.dto;

import java.util.List;

import com.hanwha.mcp.domain.model.GradleBuildAnalysis;
import com.hanwha.mcp.domain.model.GradleDependency;
import com.hanwha.mcp.domain.model.JavaPackageSummary;
import com.hanwha.mcp.domain.model.ProjectStructureAnalysis;
import com.hanwha.mcp.domain.model.TestStructureAnalysis;

public record AnalyzeProjectStructureToolResponse(
		String projectPath,
		String projectName,
		List<PackageSummaryResponse> packages,
		BuildAnalysisResponse build,
		TestStructureResponse testStructure,
		List<String> warnings) {

	public static AnalyzeProjectStructureToolResponse from(ProjectStructureAnalysis analysis) {
		return new AnalyzeProjectStructureToolResponse(
			analysis.projectPath(),
			analysis.projectName(),
			analysis.packages().stream().map(PackageSummaryResponse::from).toList(),
			BuildAnalysisResponse.from(analysis.build()),
			TestStructureResponse.from(analysis.testStructure()),
			analysis.warnings());
	}

	public record PackageSummaryResponse(String sourceSet, String packageName, long sourceFileCount) {

		static PackageSummaryResponse from(JavaPackageSummary summary) {
			return new PackageSummaryResponse(summary.sourceSet(), summary.packageName(), summary.sourceFileCount());
		}

	}

	public record BuildAnalysisResponse(
			boolean present,
			String fileName,
			String relativePath,
			boolean kotlinDsl,
			String javaVersion,
			String springBootVersion,
			List<DependencyResponse> dependencies) {

		static BuildAnalysisResponse from(GradleBuildAnalysis build) {
			return new BuildAnalysisResponse(
				build.present(),
				build.fileName(),
				build.relativePath(),
				build.kotlinDsl(),
				build.javaVersion(),
				build.springBootVersion(),
				build.dependencies().stream().map(DependencyResponse::from).toList());
		}

	}

	public record DependencyResponse(String configuration, String notation) {

		static DependencyResponse from(GradleDependency dependency) {
			return new DependencyResponse(dependency.configuration(), dependency.notation());
		}

	}

	public record TestStructureResponse(
			boolean present,
			List<String> directories,
			List<PackageSummaryResponse> packages,
			long testSourceFileCount,
			long testClassCount,
			List<String> frameworks) {

		static TestStructureResponse from(TestStructureAnalysis testStructure) {
			return new TestStructureResponse(
				testStructure.present(),
				testStructure.directories(),
				testStructure.packages().stream().map(PackageSummaryResponse::from).toList(),
				testStructure.testSourceFileCount(),
				testStructure.testClassCount(),
				testStructure.frameworks());
		}

	}

}
