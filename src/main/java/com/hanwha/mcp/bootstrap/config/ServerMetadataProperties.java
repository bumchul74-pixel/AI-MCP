package com.hanwha.mcp.bootstrap.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mcp.server.metadata")
public record ServerMetadataProperties(
		@NotBlank String name,
		@NotBlank String version,
		@NotBlank String description) {

	public ServerMetadataProperties {
		name = defaultIfBlank(name, "ai-mcp-server");
		version = defaultIfBlank(version, "0.0.1");
		description = defaultIfBlank(
				description,
				"MCP server for analyzing Java projects, querying source ontology, and generating MyBatis artifacts.");
	}

	private static String defaultIfBlank(String value, String defaultValue) {
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		return value.trim();
	}

}
