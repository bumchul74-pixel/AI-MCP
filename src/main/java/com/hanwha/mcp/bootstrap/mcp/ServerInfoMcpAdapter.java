package com.hanwha.mcp.bootstrap.mcp;

import java.util.List;
import java.util.Set;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.hanwha.mcp.application.dto.ServerInfoDetailLevel;
import com.hanwha.mcp.application.dto.ServerInfoQuery;
import com.hanwha.mcp.application.usecase.DescribeServerUseCase;
import com.hanwha.mcp.bootstrap.mcp.dto.ServerInfoToolRequest;
import com.hanwha.mcp.bootstrap.mcp.dto.ServerInfoToolResponse;
import com.hanwha.mcp.bootstrap.mcp.dto.ServerStatusPromptRequest;
import com.hanwha.mcp.common.exception.InvalidMcpInputException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ServerInfoMcpAdapter {

	private static final Logger log = LoggerFactory.getLogger(ServerInfoMcpAdapter.class);
	private static final String SERVER_INFO_TOOL = "get_server_info";
	private static final String SERVER_INFO_RESOURCE = "server://info";
	private static final String SERVER_STATUS_PROMPT = "summarize_server_status";

	private final DescribeServerUseCase describeServerUseCase;
	private final Validator validator;
	private final ObjectMapper objectMapper;
	private final MeterRegistry meterRegistry;

	public ServerInfoMcpAdapter(
			DescribeServerUseCase describeServerUseCase,
			Validator validator,
			ObjectMapper objectMapper,
			MeterRegistry meterRegistry) {
		this.describeServerUseCase = describeServerUseCase;
		this.validator = validator;
		this.objectMapper = objectMapper;
		this.meterRegistry = meterRegistry;
	}

	@McpTool(
		name = SERVER_INFO_TOOL,
		title = "Get Server Info",
		description = "Returns read-only metadata and capabilities for this MCP server.",
		generateOutputSchema = true)
	public ServerInfoToolResponse getServerInfo(
			@McpToolParam(required = false, description = "Optional detail level. Allowed values: BASIC, EXTENDED.")
			String detailLevel) {
		var sample = Timer.start(this.meterRegistry);
		try {
			var request = validate(new ServerInfoToolRequest(detailLevel));
			var response = this.describeServerUseCase.describe(new ServerInfoQuery(request.toDetailLevel()));
			this.meterRegistry.counter("mcp.tool.invocation.count", "tool", SERVER_INFO_TOOL).increment();
			log.info("mcp_tool_invoked tool={} detailLevel={}", SERVER_INFO_TOOL, response.detailLevel());
			return ServerInfoToolResponse.from(response);
		}
		catch (RuntimeException exception) {
			this.meterRegistry.counter("mcp.tool.failure.count", "tool", SERVER_INFO_TOOL).increment();
			log.warn("mcp_tool_failed tool={} reason={}", SERVER_INFO_TOOL, sanitize(exception.getMessage()));
			throw exception;
		}
		finally {
			sample.stop(Timer.builder("mcp.tool.latency").tag("tool", SERVER_INFO_TOOL).register(this.meterRegistry));
		}
	}

	@McpResource(
		name = "server-info",
		title = "Server Info",
		uri = SERVER_INFO_RESOURCE,
		description = "Read-only JSON resource describing this MCP server.",
		mimeType = MediaType.APPLICATION_JSON_VALUE)
	public String serverInfoResource() {
		this.meterRegistry.counter("mcp.resource.access.count", "resource", SERVER_INFO_RESOURCE).increment();
		var response = this.describeServerUseCase.describe(new ServerInfoQuery(ServerInfoDetailLevel.EXTENDED));
		log.info("mcp_resource_accessed resource={}", SERVER_INFO_RESOURCE);
		return toJson(ServerInfoToolResponse.from(response));
	}

	@McpPrompt(
		name = SERVER_STATUS_PROMPT,
		title = "Summarize Server Status",
		description = "Creates a prompt that asks an AI client to summarize MCP server status from server://info.")
	public McpSchema.GetPromptResult summarizeServerStatus(
			@McpArg(name = "audience", description = "Optional target audience label.", required = false)
			String audience) {
		var request = validate(new ServerStatusPromptRequest(audience));
		this.meterRegistry.counter("mcp.prompt.invocation.count", "prompt", SERVER_STATUS_PROMPT).increment();
		var prompt = "Summarize the MCP server status for the " + request.audienceOrDefault()
			+ ". Use the server://info resource as the primary source. Include available tools, resources, prompts, health, and metrics.";
		return new McpSchema.GetPromptResult(
			"Server status summary prompt",
			List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(prompt))));
	}

	private <T> T validate(T request) {
		Set<ConstraintViolation<T>> violations = this.validator.validate(request);
		if (!violations.isEmpty()) {
			throw InvalidMcpInputException.from(violations);
		}
		return request;
	}

	private String toJson(ServerInfoToolResponse response) {
		try {
			return this.objectMapper.writeValueAsString(response);
		}
		catch (JacksonException exception) {
			throw new IllegalStateException("Unable to serialize resource response", exception);
		}
	}

	private String sanitize(String message) {
		if (message == null || message.isBlank()) {
			return "unknown";
		}
		return message.replaceAll("[\\r\\n\\t]", " ");
	}

}