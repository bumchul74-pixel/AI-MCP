package com.hanwha.mcp.application.dto;

import java.util.EnumSet;
import java.util.Set;

import com.hanwha.mcp.domain.model.SourceOntologyNodeType;

public record SourceOntologySearchQuery(
		Set<SourceOntologyNodeType> nodeTypes,
		String query,
		String projectId,
		int limit) {

	private static final int DEFAULT_LIMIT = 50;
	private static final int MAX_LIMIT = 100;

	public SourceOntologySearchQuery {
		nodeTypes = nodeTypes == null || nodeTypes.isEmpty()
			? EnumSet.allOf(SourceOntologyNodeType.class)
			: EnumSet.copyOf(nodeTypes);
		query = text(query);
		projectId = text(projectId);
		if (limit <= 0) {
			limit = DEFAULT_LIMIT;
		}
		if (limit > MAX_LIMIT) {
			throw new IllegalArgumentException("limit must be at most " + MAX_LIMIT);
		}
	}

	private static String text(String value) {
		if (value == null) {
			return null;
		}
		value = value.trim();
		return value.isBlank() ? null : value;
	}

}
