package com.hanwha.mcp.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.hanwha.mcp.application.dto.ServerInfoDetailLevel;
import com.hanwha.mcp.application.dto.ServerInfoQuery;
import com.hanwha.mcp.domain.model.ServerMetadata;
import com.hanwha.mcp.infrastructure.adapter.ConfiguredServerMetadataRepository;
import org.junit.jupiter.api.Test;

class DescribeServerServiceTest {

	private final Clock clock = Clock.fixed(Instant.parse("2026-06-19T00:00:00Z"), ZoneOffset.UTC);

	@Test
	void describeReturnsBasicServerInfo() {
		var service = service();

		var response = service.describe(new ServerInfoQuery(ServerInfoDetailLevel.BASIC));
		
		System.out.println("XXX" + response.name());
		assertThat(response.name()).isEqualTo("test-mcp");
		assertThat(response.detailLevel()).isEqualTo("BASIC");
		assertThat(response.capabilities()).containsExactly("mcp-tools", "mcp-resources", "mcp-prompts");
		assertThat(response.generatedAt()).isEqualTo(Instant.parse("2026-06-19T00:00:00Z"));
	}

	@Test
	void describeReturnsExtendedServerInfo() {
		var service = service();

		var response = service.describe(new ServerInfoQuery(ServerInfoDetailLevel.EXTENDED));

		assertThat(response.detailLevel()).isEqualTo("EXTENDED");
		assertThat(response.capabilities()).contains("actuator", "metrics");
	}

	private DescribeServerService service() {
		var metadata = new ServerMetadata("test-mcp", "1.0.0", "Test MCP server");
		return new DescribeServerService(new ConfiguredServerMetadataRepository(metadata), this.clock);
	}

}