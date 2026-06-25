package com.hanwha.mcp.application.usecase;

import com.hanwha.mcp.application.dto.ProjectStructureAnalysisQuery;
import com.hanwha.mcp.domain.model.ProjectStructureAnalysis;

public interface AnalyzeProjectStructureUseCase {

	ProjectStructureAnalysis analyze(ProjectStructureAnalysisQuery query);

}
