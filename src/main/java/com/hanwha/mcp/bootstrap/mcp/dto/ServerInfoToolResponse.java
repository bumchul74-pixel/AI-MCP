package com.hanwha.mcp.bootstrap.mcp.dto;

import java.time.Instant;
import java.util.List;

import com.hanwha.mcp.application.dto.ServerInfoResponse;

public record ServerInfoToolResponse(
		String name,
		String version,
		String description,
		String detailLevel,
		List<String> capabilities,
		Instant generatedAt) {

	public static ServerInfoToolResponse from(ServerInfoResponse response) {
		return new ServerInfoToolResponse(
			response.name(),
			response.version(),
			response.description(),
			response.detailLevel(),
			response.capabilities(),
			response.generatedAt());
	}

}