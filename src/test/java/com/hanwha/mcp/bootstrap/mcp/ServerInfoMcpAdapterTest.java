package com.hanwha.mcp.bootstrap.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import tools.jackson.databind.ObjectMapper;
import com.hanwha.mcp.application.service.DescribeServerService;
import com.hanwha.mcp.common.exception.InvalidMcpInputException;
import com.hanwha.mcp.domain.model.ServerMetadata;
import com.hanwha.mcp.infrastructure.adapter.ConfiguredServerMetadataRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

class ServerInfoMcpAdapterTest {

	@Test
	void getServerInfoDefaultsToBasicDetailLevel() {
		var registry = new SimpleMeterRegistry();
		var adapter = adapter(registry);

		var response = adapter.getServerInfo(null);

		assertThat(response.detailLevel()).isEqualTo("BASIC");
		assertThat(response.capabilities()).containsExactly("mcp-tools", "mcp-resources", "mcp-prompts");
		assertThat(registry.counter("mcp.tool.invocation.count", "tool", "get_server_info").count()).isEqualTo(1.0);
	}

	@Test
	void getServerInfoRejectsUnsupportedDetailLevel() {
		var adapter = adapter(new SimpleMeterRegistry());

		assertThatThrownBy(() -> adapter.getServerInfo("FULL"))
			.isInstanceOf(InvalidMcpInputException.class)
			.hasMessageContaining("detailLevel must be BASIC or EXTENDED");
	}

	@Test
	void serverInfoResourceReturnsJson() {
		var adapter = adapter(new SimpleMeterRegistry());

		var resource = adapter.serverInfoResource();

		assertThat(resource).contains("\"detailLevel\":\"EXTENDED\"");
	}

	@Test
	void promptRejectsUnsupportedAudienceCharacters() {
		var adapter = adapter(new SimpleMeterRegistry());

		assertThatThrownBy(() -> adapter.summarizeServerStatus("ops@example.com"))
			.isInstanceOf(InvalidMcpInputException.class)
			.hasMessageContaining("audience contains unsupported characters");
	}

	private ServerInfoMcpAdapter adapter(SimpleMeterRegistry registry) {
		var metadata = new ServerMetadata("test-mcp", "1.0.0", "Test MCP server");
		var service = new DescribeServerService(
			new ConfiguredServerMetadataRepository(metadata),
			Clock.fixed(Instant.parse("2026-06-19T00:00:00Z"), ZoneOffset.UTC));
		return new ServerInfoMcpAdapter(service, Validation.buildDefaultValidatorFactory().getValidator(), new ObjectMapper(), registry);
	}

}