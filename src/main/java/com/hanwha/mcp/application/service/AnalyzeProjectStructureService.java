package com.hanwha.mcp.application.service;

import java.util.Objects;

import com.hanwha.mcp.application.dto.ProjectStructureAnalysisQuery;
import com.hanwha.mcp.application.usecase.AnalyzeProjectStructureUseCase;
import com.hanwha.mcp.domain.model.ProjectStructureAnalysis;
import com.hanwha.mcp.domain.repository.ProjectStructureAnalyzer;

public class AnalyzeProjectStructureService implements AnalyzeProjectStructureUseCase {

	private final ProjectStructureAnalyzer analyzer;

	public AnalyzeProjectStructureService(ProjectStructureAnalyzer analyzer) {
		this.analyzer = Objects.requireNonNull(analyzer);
	}

	@Override
	public ProjectStructureAnalysis analyze(ProjectStructureAnalysisQuery query) {
		var effectiveQuery = Objects.requireNonNull(query);
		return this.analyzer.analyze(effectiveQuery.projectPath());
	}

}
