package com.hanwha.mcp.bootstrap.config;

import java.time.Clock;
import java.time.Instant;

import com.hanwha.mcp.common.exception.InvalidMcpInputException;
import com.hanwha.mcp.common.exception.StructuredErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private final Clock clock;

	public GlobalExceptionHandler(Clock clock) {
		this.clock = clock;
	}

	@ExceptionHandler({ InvalidMcpInputException.class, ConstraintViolationException.class,
			MethodArgumentNotValidException.class })
	ResponseEntity<StructuredErrorResponse> handleBadRequest(Exception exception) {
		return ResponseEntity.badRequest()
			.body(error("INVALID_REQUEST", sanitize(exception.getMessage()), Instant.now(this.clock)));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<StructuredErrorResponse> handleUnexpected(Exception exception) {
		log.error("unexpected_http_error type={}", exception.getClass().getSimpleName());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(error("INTERNAL_ERROR", "Unexpected server error", Instant.now(this.clock)));
	}

	private StructuredErrorResponse error(String code, String message, Instant timestamp) {
		return new StructuredErrorResponse(code, message, timestamp);
	}

	private String sanitize(String message) {
		if (message == null || message.isBlank()) {
			return "Invalid request";
		}
		return message.replaceAll("[\\r\\n\\t]", " ");
	}

}