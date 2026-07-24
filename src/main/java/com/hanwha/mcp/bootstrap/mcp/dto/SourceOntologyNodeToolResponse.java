package com.hanwha.mcp.bootstrap.mcp.dto;

import com.hanwha.mcp.domain.model.SourceOntologyNode;

public record SourceOntologyNodeToolResponse(
		String id,
		String nodeType,
		String name,
		String projectId,
		String filePath,
		String fullyQualifiedName,
		String signature,
		String declaringType,
		String httpMethod,
		String endpointPath,
		String namespace,
		String statementId,
		String sql,
		String operation) {

	public static SourceOntologyNodeToolResponse from(SourceOntologyNode node) {
		return new SourceOntologyNodeToolResponse(
			node.id(),
			node.nodeType().name(),
			node.name(),
			node.projectId(),
			node.filePath(),
			node.fullyQualifiedName(),
			node.signature(),
			node.declaringType(),
			node.httpMethod(),
			node.endpointPath(),
			node.namespace(),
			node.statementId(),
			node.sql(),
			node.operation());
	}

}
