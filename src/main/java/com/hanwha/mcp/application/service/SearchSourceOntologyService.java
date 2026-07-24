package com.hanwha.mcp.application.service;

import java.util.List;
import java.util.Objects;

import com.hanwha.mcp.application.dto.SourceOntologySearchQuery;
import com.hanwha.mcp.application.usecase.SearchSourceOntologyUseCase;
import com.hanwha.mcp.domain.model.SourceOntologyNode;
import com.hanwha.mcp.domain.repository.SourceOntologyRepository;

public class SearchSourceOntologyService implements SearchSourceOntologyUseCase {

	private final SourceOntologyRepository repository;

	public SearchSourceOntologyService(SourceOntologyRepository repository) {
		this.repository = Objects.requireNonNull(repository);
	}

	@Override
	public List<SourceOntologyNode> search(SourceOntologySearchQuery query) {
		var effectiveQuery = Objects.requireNonNull(query, "query must not be null");
		return this.repository.search(
			effectiveQuery.nodeTypes(),
			effectiveQuery.query(),
			effectiveQuery.projectId(),
			effectiveQuery.limit());
	}

}
