package com.hanwha.mcp.application.dto;

import java.util.Objects;

public record ServerInfoQuery(ServerInfoDetailLevel detailLevel) {

	public ServerInfoQuery {
		detailLevel = Objects.requireNonNullElse(detailLevel, ServerInfoDetailLevel.BASIC);
	}

}