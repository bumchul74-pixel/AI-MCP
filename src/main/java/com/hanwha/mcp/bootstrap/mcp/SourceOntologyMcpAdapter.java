package com.hanwha.mcp.bootstrap.mcp;

import java.util.List;
import java.util.Set;

import com.hanwha.mcp.application.usecase.SearchSourceOntologyUseCase;
import com.hanwha.mcp.bootstrap.mcp.dto.SourceOntologySearchToolRequest;
import com.hanwha.mcp.bootstrap.mcp.dto.SourceOntologySearchToolResponse;
import com.hanwha.mcp.common.exception.InvalidMcpInputException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class SourceOntologyMcpAdapter {

	private static final Logger log = LoggerFactory.getLogger(SourceOntologyMcpAdapter.class);
	private static final String SEARCH_SOURCE_ONTOLOGY_TOOL = "search_source_ontology";

	private final SearchSourceOntologyUseCase searchSourceOntologyUseCase;
	private final Validator validator;
	private final MeterRegistry meterRegistry;

	public SourceOntologyMcpAdapter(
			SearchSourceOntologyUseCase searchSourceOntologyUseCase,
			Validator validator,
			MeterRegistry meterRegistry) {
		this.searchSourceOntologyUseCase = searchSourceOntologyUseCase;
		this.validator = validator;
		this.meterRegistry = meterRegistry;
	}

	@McpTool(
		name = SEARCH_SOURCE_ONTOLOGY_TOOL,
		title = "Search Java Source Ontology",
		description = "Searches read-only Neo4j source ontology nodes for Java types, methods, fields, API endpoints, and MyBatis SQL statements.",
		generateOutputSchema = true)
	public SourceOntologySearchToolResponse searchSourceOntology(
			@McpToolParam(required = false, description = "Optional node types: JAVA_TYPE, METHOD, FIELD, API_ENDPOINT, SQL_STATEMENT. Defaults to all types.")
			List<String> nodeTypes,
			@McpToolParam(required = false, description = "Optional case-insensitive keyword matched against names, FQNs, paths, signatures, and SQL.")
			String query,
			@McpToolParam(required = false, description = "Optional exact Neo4j projectId filter, for example management.")
			String projectId,
			@McpToolParam(required = false, description = "Maximum results from 1 to 100. Defaults to 50.")
			Integer limit) {
		var sample = Timer.start(this.meterRegistry);
		try {
			var request = validate(new SourceOntologySearchToolRequest(nodeTypes, query, projectId, limit));
			var searchQuery = request.toQuery();
			var response = SourceOntologySearchToolResponse.from(
				searchQuery,
				this.searchSourceOntologyUseCase.search(searchQuery));
			this.meterRegistry.counter("mcp.tool.invocation.count", "tool", SEARCH_SOURCE_ONTOLOGY_TOOL).increment();
			log.info(
				"mcp_tool_invoked tool={} nodeTypes={} projectId={} resultCount={}",
				SEARCH_SOURCE_ONTOLOGY_TOOL,
				response.nodeTypes(),
				response.projectId(),
				response.count());
			return response;
		}
		catch (IllegalArgumentException exception) {
			this.meterRegistry.counter("mcp.tool.failure.count", "tool", SEARCH_SOURCE_ONTOLOGY_TOOL).increment();
			log.warn("mcp_tool_failed tool={} reason={}", SEARCH_SOURCE_ONTOLOGY_TOOL, sanitize(exception.getMessage()));
			throw new InvalidMcpInputException(exception.getMessage());
		}
		catch (RuntimeException exception) {
			this.meterRegistry.counter("mcp.tool.failure.count", "tool", SEARCH_SOURCE_ONTOLOGY_TOOL).increment();
			log.warn("mcp_tool_failed tool={} reason={}", SEARCH_SOURCE_ONTOLOGY_TOOL, sanitize(exception.getMessage()));
			throw exception;
		}
		finally {
			sample.stop(Timer.builder("mcp.tool.latency")
				.tag("tool", SEARCH_SOURCE_ONTOLOGY_TOOL)
				.register(this.meterRegistry));
		}
	}

	private <T> T validate(T request) {
		Set<ConstraintViolation<T>> violations = this.validator.validate(request);
		if (!violations.isEmpty()) {
			throw InvalidMcpInputException.from(violations);
		}
		return request;
	}

	private String sanitize(String message) {
		if (message == null || message.isBlank()) {
			return "unknown";
		}
		return message.replaceAll("[\\r\\n\\t]", " ");
	}

}
