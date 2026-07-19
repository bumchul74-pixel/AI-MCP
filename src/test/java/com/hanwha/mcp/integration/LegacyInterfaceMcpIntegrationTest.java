package com.hanwha.mcp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.hanwha.mcp.bootstrap.AiMcpApplication;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Tag("docker-integration")
@SpringBootTest(classes = AiMcpApplication.class, properties = {
	"debug=false",
	"spring.ai.mcp.server.enabled=false",
	"spring.ai.mcp.client.enabled=true",
	"spring.ai.mcp.client.initialized=true",
	"spring.ai.mcp.client.type=SYNC"
})
class LegacyInterfaceMcpIntegrationTest {

	private static final String DEFAULT_ENDPOINT = "http://localhost:18090/mcp";

	@Autowired
	@Qualifier("mcpSyncClients")
	private List<McpSyncClient> mcpSyncClients;

	@DynamicPropertySource
	static void legacyInterfaceProperties(DynamicPropertyRegistry registry) {
		var endpoint = configuredEndpoint();
		registry.add("spring.ai.mcp.client.streamable-http.connections.secure-coding-mcp.url",
			() -> endpoint.getScheme() + "://" + endpoint.getAuthority());
		registry.add("spring.ai.mcp.client.streamable-http.connections.secure-coding-mcp.endpoint",
			() -> endpoint.getPath().isBlank() ? "/mcp" : endpoint.getPath());
	}

	@Test
	void createsInitializedMcpClientInSpringContext() {
		var client = legacyInterfaceClient();

		assertThat(client.isInitialized()).isTrue();
		assertThat(client.getServerInfo().name()).isEqualTo("java-secure-coding-mcp");
	}

	@Test
	void exposesExpectedSecureCodingTools() {
		var toolNames = legacyInterfaceClient().listTools()
			.tools()
			.stream()
			.map(tool -> tool.name())
			.toList();

		assertThat(toolNames).contains("scan_project", "scan_file", "scan_source", "list_rules");
	}

	@Test
	void callsScanSourceToolThroughSpringManagedClient() {
		var result = legacyInterfaceClient().callTool(new CallToolRequest("scan_source", Map.of(
			"fileName", "Sample.java",
			"source", "public class Sample {}",
			"ruleSets", List.of()
		)));

		assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
		assertThat(result.content()).isNotEmpty();
	}

	@Test
	void callsListRulesToolThroughSpringManagedClient() {
		var result = legacyInterfaceClient().callTool(new CallToolRequest("list_rules", Map.of()));

		assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
		assertThat(result.content()).isNotEmpty();
	}

	private McpSyncClient legacyInterfaceClient() {
		assertThat(this.mcpSyncClients)
			.as("Spring AI should configure exactly one legacy-interface MCP client")
			.hasSize(1);
		return this.mcpSyncClients.getFirst();
	}

	private static URI configuredEndpoint() {
		return URI.create(System.getProperty("legacy.interface.mcp.url", DEFAULT_ENDPOINT));
	}
}
