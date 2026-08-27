package com.hanwha.mcp.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpClient;

import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class McpHttpClientConfigurationTest {

	@Test
	void forcesStreamableHttpTransportToUseHttp11() {
		var configuration = new McpHttpClientConfiguration();
		var builder = HttpClientStreamableHttpTransport.builder("http://localhost:8002");

		configuration.mcpHttp11Customizer().customize("ppt-mcp", builder);
		var transport = builder.build();
		var httpClient = (HttpClient) ReflectionTestUtils.getField(transport, "httpClient");

		assertThat(httpClient).isNotNull();
		assertThat(httpClient.version()).isEqualTo(HttpClient.Version.HTTP_1_1);
	}
}
