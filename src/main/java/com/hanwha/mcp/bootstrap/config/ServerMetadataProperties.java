package com.hanwha.mcp.bootstrap.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mcp.server.metadata")
public record ServerMetadataProperties(
		@NotBlank String name,
		@NotBlank String version,
		@NotBlank String description) {
}