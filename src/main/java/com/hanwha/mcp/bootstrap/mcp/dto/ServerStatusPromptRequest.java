package com.hanwha.mcp.bootstrap.mcp.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ServerStatusPromptRequest(
		@Size(max = 64, message = "audience must be at most 64 characters")
		@Pattern(regexp = "[A-Za-z0-9 ._-]*", message = "audience contains unsupported characters")
		String audience) {

	public ServerStatusPromptRequest {
		if (audience != null) {
			audience = audience.trim();
		}
	}

	public String audienceOrDefault() {
		if (this.audience == null || this.audience.isBlank()) {
			return "operations team";
		}
		return this.audience;
	}

}