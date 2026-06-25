package com.hanwha.mcp.application.dto;

import java.time.Instant;
import java.util.List;

public record ServerInfoResponse(
		String name,
		String version,
		String description,
		String detailLevel,
		List<String> capabilities,
		Instant generatedAt) {

	public ServerInfoResponse {
		capabilities = List.copyOf(capabilities);
	}

}