package com.hanwha.mcp.bootstrap.rest;

import java.time.Clock;
import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class PingRestController {

	private final Clock clock;

	PingRestController(Clock clock) {
		this.clock = clock;
	}

	@GetMapping("/ping")
	PingResponse ping() {
		return new PingResponse("OK", "pong", Instant.now(this.clock));
	}

	record PingResponse(String status, String message, Instant timestamp) {
	}

}
