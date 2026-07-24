package com.hanwha.mcp.domain.model;

import java.util.Arrays;
import java.util.Locale;

public enum SourceOntologyNodeType {

	JAVA_TYPE("JavaType"),
	METHOD("Method"),
	FIELD("Field"),
	API_ENDPOINT("ApiEndpoint"),
	SQL_STATEMENT("SqlStatement");

	private final String neo4jLabel;

	SourceOntologyNodeType(String neo4jLabel) {
		this.neo4jLabel = neo4jLabel;
	}

	public String neo4jLabel() {
		return this.neo4jLabel;
	}

	public static SourceOntologyNodeType from(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("nodeTypes must not contain blank values");
		}
		var normalized = value.trim()
			.replace('-', '_')
			.replace(' ', '_')
			.toUpperCase(Locale.ROOT);
		return Arrays.stream(values())
			.filter(type -> type.name().equals(normalized)
				|| type.neo4jLabel.toUpperCase(Locale.ROOT).equals(normalized.replace("_", "")))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(
				"Unsupported node type: " + value
					+ ". Allowed values: JAVA_TYPE, METHOD, FIELD, API_ENDPOINT, SQL_STATEMENT"));
	}

}
