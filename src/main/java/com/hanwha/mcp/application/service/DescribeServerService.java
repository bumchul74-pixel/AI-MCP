package com.hanwha.mcp.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.hanwha.mcp.application.dto.ServerInfoDetailLevel;
import com.hanwha.mcp.application.dto.ServerInfoQuery;
import com.hanwha.mcp.application.dto.ServerInfoResponse;
import com.hanwha.mcp.application.usecase.DescribeServerUseCase;
import com.hanwha.mcp.domain.repository.ServerMetadataRepository;

public class DescribeServerService implements DescribeServerUseCase {

	private final ServerMetadataRepository repository;
	private final Clock clock;

	public DescribeServerService(ServerMetadataRepository repository, Clock clock) {
		this.repository = Objects.requireNonNull(repository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Override
	public ServerInfoResponse describe(ServerInfoQuery query) {
		var effectiveQuery = Objects.requireNonNullElse(query, new ServerInfoQuery(ServerInfoDetailLevel.BASIC));
		var metadata = this.repository.getServerMetadata();
		return new ServerInfoResponse(
			metadata.name(),
			metadata.version(),
			metadata.description(),
			effectiveQuery.detailLevel().name(),
			capabilities(effectiveQuery.detailLevel()),
			Instant.now(this.clock));
	}

	private List<String> capabilities(ServerInfoDetailLevel detailLevel) {
		return switch (detailLevel) {
			case BASIC -> List.of("mcp-tools", "mcp-resources", "mcp-prompts");
			case EXTENDED -> List.of("mcp-tools", "mcp-resources", "mcp-prompts", "actuator", "metrics");
		};
	}

}