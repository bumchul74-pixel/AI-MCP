package com.hanwha.mcp.common.exception;

import java.time.Instant;

public record StructuredErrorResponse(String code, String message, Instant timestamp) {
}