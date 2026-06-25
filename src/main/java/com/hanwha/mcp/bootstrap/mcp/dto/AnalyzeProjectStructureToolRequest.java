package com.hanwha.mcp.bootstrap.mcp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AnalyzeProjectStructureToolRequest(
		@NotBlank(message = "projectPath must not be blank")
		@Size(max = 512, message = "projectPath must be at most 512 characters")
		@Pattern(regexp = "^[^\\r\\n\\t\\u0000]+$", message = "projectPath contains unsupported characters")
		String projectPath) {

	public AnalyzeProjectStructureToolRequest {
		if (projectPath != null) {
			projectPath = projectPath.trim();
		}
	}

}
