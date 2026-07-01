package com.hanwha.mcp.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class PostgreSqlDriverClasspathTest {

	@Test
	void postgresqlDriverIsAvailableOnRuntimeClasspath() {
		assertDoesNotThrow(() -> Class.forName("org.postgresql.Driver"));
	}

}