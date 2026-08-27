package com.hanwha.mcp.bootstrap.config;

import java.net.http.HttpClient;

import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpHttpClientConfiguration {

	@Bean
	McpClientCustomizer<HttpClientStreamableHttpTransport.Builder> mcpHttp11Customizer() {
		return (name, builder) -> builder.customizeClient(
			client -> client.version(HttpClient.Version.HTTP_1_1));
	}
}
