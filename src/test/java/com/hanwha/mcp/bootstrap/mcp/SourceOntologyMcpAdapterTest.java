package com.hanwha.mcp.bootstrap.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.hanwha.mcp.application.dto.SourceOntologySearchQuery;
import com.hanwha.mcp.application.usecase.SearchSourceOntologyUseCase;
import com.hanwha.mcp.common.exception.InvalidMcpInputException;
import com.hanwha.mcp.domain.model.SourceOntologyNode;
import com.hanwha.mcp.domain.model.SourceOntologyNodeType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

class SourceOntologyMcpAdapterTest {

	@Test
	void searchesSelectedOntologyNodeTypes() {
		var registry = new SimpleMeterRegistry();
		var adapter = adapter(registry);

		var response = adapter.searchSourceOntology(
			List.of("JAVA_TYPE", "API_ENDPOINT"),
			" user ",
			"management",
			20);

		assertThat(response.count()).isEqualTo(1);
		assertThat(response.nodeTypes()).containsExactly("API_ENDPOINT", "JAVA_TYPE");
		assertThat(response.nodes().getFirst().fullyQualifiedName()).isEqualTo("com.example.UserService");
		assertThat(registry.counter("mcp.tool.invocation.count", "tool", "search_source_ontology").count())
			.isEqualTo(1.0);
	}

	@Test
	void rejectsUnsupportedNodeType() {
		var adapter = adapter(new SimpleMeterRegistry());

		assertThatThrownBy(() -> adapter.searchSourceOntology(List.of("DOCUMENT"), null, null, null))
			.isInstanceOf(InvalidMcpInputException.class)
			.hasMessageContaining("Unsupported node type");
	}

	@Test
	void rejectsLimitAboveMaximum() {
		var adapter = adapter(new SimpleMeterRegistry());

		assertThatThrownBy(() -> adapter.searchSourceOntology(null, null, null, 101))
			.isInstanceOf(InvalidMcpInputException.class)
			.hasMessageContaining("limit must be at most 100");
	}

	private SourceOntologyMcpAdapter adapter(SimpleMeterRegistry registry) {
		SearchSourceOntologyUseCase useCase = this::search;
		return new SourceOntologyMcpAdapter(
			useCase,
			Validation.buildDefaultValidatorFactory().getValidator(),
			registry);
	}

	private List<SourceOntologyNode> search(SourceOntologySearchQuery query) {
		return List.of(new SourceOntologyNode(
			"type:management:com.example.UserService",
			SourceOntologyNodeType.JAVA_TYPE,
			"UserService",
			"management",
			"src/main/java/com/example/UserService.java",
			"com.example.UserService",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			""));
	}

}
