package com.hanwha.mcp.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest(properties = "spring.ai.mcp.client.enabled=false")
class DownstreamMcpConfigurationTest {

	@Autowired
	private Environment environment;

	@Test
	void configuresDownstreamMcpServers() {
		assertThat(environment.getProperty(
			"spring.ai.mcp.client.streamable-http.connections.easyocr-mcp.url"))
			.isEqualTo("http://localhost:8001");
		assertThat(environment.getProperty(
			"spring.ai.mcp.client.streamable-http.connections.easyocr-mcp.endpoint"))
			.isEqualTo("/ocr");
		assertThat(environment.getProperty(
			"spring.ai.mcp.client.streamable-http.connections.ppt-mcp.url"))
			.isEqualTo("http://localhost:8002");
		assertThat(environment.getProperty(
			"spring.ai.mcp.client.streamable-http.connections.ppt-mcp.endpoint"))
			.isEqualTo("/ppt");
		assertThat(environment.getProperty(
			"spring.ai.mcp.client.request-timeout"))
			.isEqualTo("900s");
		assertThat(environment.getProperty(
			"spring.ai.mcp.server.expose-mcp-client-tools", Boolean.class))
			.isTrue();
	}
}
