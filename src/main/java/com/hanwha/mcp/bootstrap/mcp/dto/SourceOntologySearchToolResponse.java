package com.hanwha.mcp.bootstrap.mcp.dto;

import java.util.List;

import com.hanwha.mcp.application.dto.SourceOntologySearchQuery;
import com.hanwha.mcp.domain.model.SourceOntologyNode;

public record SourceOntologySearchToolResponse(
		String query,
		String projectId,
		List<String> nodeTypes,
		int count,
		List<SourceOntologyNodeToolResponse> nodes) {

	public static SourceOntologySearchToolResponse from(
			SourceOntologySearchQuery query,
			List<SourceOntologyNode> nodes) {
		var responses = nodes.stream().map(SourceOntologyNodeToolResponse::from).toList();
		return new SourceOntologySearchToolResponse(
			query.query() == null ? "" : query.query(),
			query.projectId() == null ? "" : query.projectId(),
			query.nodeTypes().stream().map(Enum::name).sorted().toList(),
			responses.size(),
			responses);
	}

}
