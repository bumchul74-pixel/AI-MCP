package com.hanwha.mcp.bootstrap.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class PingRestControllerTest {

	@Test
	void pingReturnsSimpleStatusResponse() {
		var controller = new PingRestController(
			Clock.fixed(Instant.parse("2026-07-03T00:00:00Z"), ZoneOffset.UTC));

		var response = controller.ping();

		assertThat(response.status()).isEqualTo("OK");
		assertThat(response.message()).isEqualTo("pong");
		assertThat(response.timestamp()).isEqualTo(Instant.parse("2026-07-03T00:00:00Z"));
	}

}
