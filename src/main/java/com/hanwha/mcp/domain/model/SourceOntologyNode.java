package com.hanwha.mcp.domain.model;

public record SourceOntologyNode(
		String id,
		SourceOntologyNodeType nodeType,
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

	public SourceOntologyNode {
		if (id == null || id.isBlank()) {
			throw new IllegalArgumentException("id must not be blank");
		}
		if (nodeType == null) {
			throw new IllegalArgumentException("nodeType must not be null");
		}
		id = id.trim();
		name = text(name);
		projectId = text(projectId);
		filePath = text(filePath);
		fullyQualifiedName = text(fullyQualifiedName);
		signature = text(signature);
		declaringType = text(declaringType);
		httpMethod = text(httpMethod);
		endpointPath = text(endpointPath);
		namespace = text(namespace);
		statementId = text(statementId);
		sql = text(sql);
		operation = text(operation);
	}

	private static String text(String value) {
		return value == null ? "" : value.trim();
	}

}
