package com.hanwha.mcp.bootstrap.mcp.dto;

import java.util.EnumSet;
import java.util.List;

import com.hanwha.mcp.application.dto.SourceOntologySearchQuery;
import com.hanwha.mcp.domain.model.SourceOntologyNodeType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SourceOntologySearchToolRequest(
		@Size(max = 5, message = "nodeTypes must contain at most 5 values")
		List<@Pattern(
			regexp = "^[A-Za-z_-]+$",
			message = "nodeTypes contains an unsupported value") String> nodeTypes,

		@Size(max = 256, message = "query must be at most 256 characters")
		@Pattern(regexp = "^[^\\r\\n\\t\\u0000]*$", message = "query contains unsupported characters")
		String query,

		@Size(max = 128, message = "projectId must be at most 128 characters")
		@Pattern(regexp = "^[A-Za-z0-9._:-]*$", message = "projectId contains unsupported characters")
		String projectId,

		@Min(value = 1, message = "limit must be at least 1")
		@Max(value = 100, message = "limit must be at most 100")
		Integer limit) {

	public SourceOntologySearchToolRequest {
		nodeTypes = nodeTypes == null
			? List.of()
			: nodeTypes.stream().map(SourceOntologySearchToolRequest::trim).toList();
		query = text(query);
		projectId = text(projectId);
	}

	public SourceOntologySearchQuery toQuery() {
		var types = this.nodeTypes.isEmpty()
			? EnumSet.allOf(SourceOntologyNodeType.class)
			: this.nodeTypes.stream()
				.map(SourceOntologyNodeType::from)
				.collect(() -> EnumSet.noneOf(SourceOntologyNodeType.class), EnumSet::add, EnumSet::addAll);
		return new SourceOntologySearchQuery(types, this.query, this.projectId, this.limit == null ? 50 : this.limit);
	}

	private static String trim(String value) {
		return value == null ? null : value.trim();
	}

	private static String text(String value) {
		if (value == null) {
			return null;
		}
		value = value.trim();
		return value.isBlank() ? null : value;
	}

}
