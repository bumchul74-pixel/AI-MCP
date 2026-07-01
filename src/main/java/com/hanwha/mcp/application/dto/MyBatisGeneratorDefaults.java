package com.hanwha.mcp.application.dto;

public record MyBatisGeneratorDefaults(String basePackage, String defaultSchema) {

	public MyBatisGeneratorDefaults {
		if (basePackage == null || basePackage.isBlank()) {
			basePackage = "com.example.app";
		}
		basePackage = basePackage.trim();
		if (defaultSchema != null) {
			defaultSchema = defaultSchema.trim();
			if (defaultSchema.isBlank()) {
				defaultSchema = null;
			}
		}
	}

}