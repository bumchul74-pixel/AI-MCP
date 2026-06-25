package com.hanwha.mcp.common.exception;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;

public class InvalidMcpInputException extends RuntimeException {

	public InvalidMcpInputException(String message) {
		super(message);
	}

	public static InvalidMcpInputException from(Set<? extends ConstraintViolation<?>> violations) {
		var message = violations.stream()
			.map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
			.sorted()
			.collect(Collectors.joining("; "));
		return new InvalidMcpInputException(message);
	}

}