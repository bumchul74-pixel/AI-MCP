package com.hanwha.mcp.bootstrap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mcp.mybatis.datasource")
public record MyBatisDataSourceProperties(
		String url,
		String username,
		String password,
		String driverClassName) {

	public MyBatisDataSourceProperties {
		url = normalize(url);
		username = normalize(username);
		driverClassName = normalize(driverClassName);
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		value = value.trim();
		return value.isBlank() ? null : value;
	}

}