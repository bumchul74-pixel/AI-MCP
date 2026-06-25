package com.hanwha.mcp.bootstrap.mcp;

import java.util.Set;

import com.hanwha.mcp.application.dto.ProjectStructureAnalysisQuery;
import com.hanwha.mcp.application.usecase.AnalyzeProjectStructureUseCase;
import com.hanwha.mcp.bootstrap.mcp.dto.AnalyzeProjectStructureToolRequest;
import com.hanwha.mcp.bootstrap.mcp.dto.AnalyzeProjectStructureToolResponse;
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
public class ProjectStructureMcpAdapter {

	private static final Logger log = LoggerFactory.getLogger(ProjectStructureMcpAdapter.class);
	private static final String ANALYZE_PROJECT_STRUCTURE_TOOL = "analyze_project_structure";

	private final AnalyzeProjectStructureUseCase analyzeProjectStructureUseCase;
	private final Validator validator;
	private final MeterRegistry meterRegistry;

	public ProjectStructureMcpAdapter(
			AnalyzeProjectStructureUseCase analyzeProjectStructureUseCase,
			Validator validator,
			MeterRegistry meterRegistry) {
		this.analyzeProjectStructureUseCase = analyzeProjectStructureUseCase;
		this.validator = validator;
		this.meterRegistry = meterRegistry;
	}

	@McpTool(
		name = ANALYZE_PROJECT_STRUCTURE_TOOL,
		title = "Analyze Project Structure",
 		generateOutputSchema = true)
	public AnalyzeProjectStructureToolResponse analyzeProjectStructure(
			@McpToolParam(required = true, description = "Absolute or relative path to the project directory to analyze.")
			String projectPath) {
		var sample = Timer.start(this.meterRegistry);
		try {
			var request = validate(new AnalyzeProjectStructureToolRequest(projectPath));
			var response = AnalyzeProjectStructureToolResponse.from(
				this.analyzeProjectStructureUseCase.analyze(new ProjectStructureAnalysisQuery(request.projectPath())));
			this.meterRegistry.counter("mcp.tool.invocation.count", "tool", ANALYZE_PROJECT_STRUCTURE_TOOL).increment();
			log.info(
				"mcp_tool_invoked tool={} projectName={} packageCount={}",
				ANALYZE_PROJECT_STRUCTURE_TOOL,
				response.projectName(),
				response.packages().size());
			return response;
		}
		catch (IllegalArgumentException exception) {
			this.meterRegistry.counter("mcp.tool.failure.count", "tool", ANALYZE_PROJECT_STRUCTURE_TOOL).increment();
			log.warn(
				"mcp_tool_failed tool={} reason={}",
				ANALYZE_PROJECT_STRUCTURE_TOOL,
				sanitize(exception.getMessage()));
			throw new InvalidMcpInputException(exception.getMessage());
		}
		catch (RuntimeException exception) {
			this.meterRegistry.counter("mcp.tool.failure.count", "tool", ANALYZE_PROJECT_STRUCTURE_TOOL).increment();
			log.warn(
				"mcp_tool_failed tool={} reason={}",
				ANALYZE_PROJECT_STRUCTURE_TOOL,
				sanitize(exception.getMessage()));
			throw exception;
		}
		finally {
			sample.stop(Timer.builder("mcp.tool.latency")
				.tag("tool", ANALYZE_PROJECT_STRUCTURE_TOOL)
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
