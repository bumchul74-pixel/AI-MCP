package com.hanwha.mcp.application.dto;

public record ProjectStructureAnalysisQuery(String projectPath) {

	public ProjectStructureAnalysisQuery {
		if (projectPath == null || projectPath.isBlank()) {
			throw new IllegalArgumentException("projectPath must not be blank");
		}
		projectPath = projectPath.trim();
	}

}
