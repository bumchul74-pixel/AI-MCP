package com.hanwha.mcp.bootstrap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mcp.mybatis.generator")
public record MyBatisGeneratorProperties(
		String basePackage,
		String defaultSchema) {

	public MyBatisGeneratorProperties {
		if (basePackage == null || basePackage.isBlank()) {
			basePackage = "com.example.app";
		}
		else {
			basePackage = basePackage.trim();
		}
		if (defaultSchema != null) {
			defaultSchema = defaultSchema.trim();
			if (defaultSchema.isBlank()) {
				defaultSchema = null;
			}
		}
	}

}