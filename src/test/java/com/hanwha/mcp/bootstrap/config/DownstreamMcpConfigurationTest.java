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
	void configuresEasyOcrAsDownstreamMcpServer() {
		assertThat(environment.getProperty(
			"spring.ai.mcp.client.streamable-http.connections.easyocr-mcp.url"))
			.isEqualTo("http://localhost:8001");
		assertThat(environment.getProperty(
			"spring.ai.mcp.client.streamable-http.connections.easyocr-mcp.endpoint"))
			.isEqualTo("/ocr");
		assertThat(environment.getProperty(
			"spring.ai.mcp.server.expose-mcp-client-tools", Boolean.class))
			.isTrue();
	}
}
