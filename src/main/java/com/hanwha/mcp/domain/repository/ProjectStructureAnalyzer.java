package com.hanwha.mcp.domain.repository;

import com.hanwha.mcp.domain.model.ProjectStructureAnalysis;

public interface ProjectStructureAnalyzer {

	ProjectStructureAnalysis analyze(String projectPath);

}
