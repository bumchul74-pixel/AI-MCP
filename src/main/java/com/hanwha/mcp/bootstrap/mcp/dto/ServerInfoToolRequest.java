package com.hanwha.mcp.bootstrap.mcp.dto;

import java.util.Locale;

import com.hanwha.mcp.application.dto.ServerInfoDetailLevel;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ServerInfoToolRequest(
		@Size(max = 16, message = "detailLevel must be at most 16 characters")
		@Pattern(regexp = "BASIC|EXTENDED", flags = Pattern.Flag.CASE_INSENSITIVE,
				message = "detailLevel must be BASIC or EXTENDED")
		String detailLevel) {

	public ServerInfoToolRequest {
		if (detailLevel != null) {
			detailLevel = detailLevel.trim();
		}
	}

	public ServerInfoDetailLevel toDetailLevel() {
		if (this.detailLevel == null) {
			return ServerInfoDetailLevel.BASIC;
		}
		return ServerInfoDetailLevel.valueOf(this.detailLevel.toUpperCase(Locale.ROOT));
	}

}