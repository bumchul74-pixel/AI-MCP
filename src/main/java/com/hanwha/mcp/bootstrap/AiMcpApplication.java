package com.hanwha.mcp.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.hanwha.mcp")
@ConfigurationPropertiesScan(basePackages = "com.hanwha.mcp")
public class AiMcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiMcpApplication.class, args);
	}

}