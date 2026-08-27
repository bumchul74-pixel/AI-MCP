package com.hanwha.mcp.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

class DownstreamMcpSafeDefaultTest {

	@Test
	void disablesDownstreamMcpClientsByDefault() throws IOException {
		var propertySources = new YamlPropertySourceLoader()
			.load("application", new ClassPathResource("application.yml"));

		var configuredValue = propertySources.stream()
			.map(propertySource -> propertySource.getProperty("spring.ai.mcp.client.enabled"))
			.filter(value -> value != null)
			.findFirst()
			.orElseThrow();

		assertThat(configuredValue)
			.isEqualTo("${DOWNSTREAM_MCP_ENABLED:${SECURE_CODING_MCP_ENABLED:false}}");
	}
}