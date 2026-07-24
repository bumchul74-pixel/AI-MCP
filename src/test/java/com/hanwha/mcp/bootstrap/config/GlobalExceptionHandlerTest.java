package com.hanwha.mcp.bootstrap.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

class GlobalExceptionHandlerTest {

	@Test
	void doesNotWriteAnErrorBodyAfterAsyncClientDisconnects() {
		var handler = new GlobalExceptionHandler(Clock.systemUTC());
		var exception = new AsyncRequestNotUsableException("Client disconnected");

		assertThatCode(() -> handler.handleDisconnectedClient(exception))
			.doesNotThrowAnyException();
	}

}
