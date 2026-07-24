package com.hanwha.mcp.bootstrap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mcp.ontology.neo4j")
public record Neo4jOntologyProperties(
		String uri,
		String username,
		String password,
		String database) {

	public Neo4jOntologyProperties {
		uri = defaultValue(uri, "bolt://localhost:7687");
		username = defaultValue(username, "neo4j");
		database = defaultValue(database, "neo4j");
	}

	private static String defaultValue(String value, String defaultValue) {
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		return value.trim();
	}

}
