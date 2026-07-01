package com.hanwha.mcp.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServerMetadataPropertiesTest {

	@Test
	void defaultsMissingMetadataValues() {
		var properties = new ServerMetadataProperties(null, " ", null);

		assertThat(properties.name()).isEqualTo("ai-mcp-server");
		assertThat(properties.version()).isEqualTo("0.0.1");
		assertThat(properties.description())
				.isEqualTo("MCP server for analyzing Gradle/Spring Boot projects and generating MyBatis artifacts from database metadata.");
	}

	@Test
	void trimsConfiguredMetadataValues() {
		var properties = new ServerMetadataProperties(" custom-mcp ", " 1.2.3 ", " Custom server ");

		assertThat(properties.name()).isEqualTo("custom-mcp");
		assertThat(properties.version()).isEqualTo("1.2.3");
		assertThat(properties.description()).isEqualTo("Custom server");
	}

}
