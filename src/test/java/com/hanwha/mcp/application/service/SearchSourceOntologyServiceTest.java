package com.hanwha.mcp.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import com.hanwha.mcp.application.dto.SourceOntologySearchQuery;
import com.hanwha.mcp.domain.model.SourceOntologyNode;
import com.hanwha.mcp.domain.model.SourceOntologyNodeType;
import com.hanwha.mcp.domain.repository.SourceOntologyRepository;
import org.junit.jupiter.api.Test;

class SearchSourceOntologyServiceTest {

	@Test
	void delegatesNormalizedFiltersToRepository() {
		var repository = new CapturingRepository();
		var service = new SearchSourceOntologyService(repository);
		var query = new SourceOntologySearchQuery(
			Set.of(SourceOntologyNodeType.JAVA_TYPE),
			" UserService ",
			" management ",
			25);

		var result = service.search(query);

		assertThat(result).hasSize(1);
		assertThat(repository.query).isEqualTo("UserService");
		assertThat(repository.projectId).isEqualTo("management");
		assertThat(repository.limit).isEqualTo(25);
		assertThat(repository.nodeTypes).containsExactly(SourceOntologyNodeType.JAVA_TYPE);
	}

	@Test
	void defaultsMissingTypesAndLimit() {
		var query = new SourceOntologySearchQuery(Set.of(), null, null, 0);

		assertThat(query.nodeTypes()).containsExactlyInAnyOrder(SourceOntologyNodeType.values());
		assertThat(query.limit()).isEqualTo(50);
	}

	private static final class CapturingRepository implements SourceOntologyRepository {

		private Set<SourceOntologyNodeType> nodeTypes;
		private String query;
		private String projectId;
		private int limit;

		@Override
		public List<SourceOntologyNode> search(
				Set<SourceOntologyNodeType> nodeTypes,
				String query,
				String projectId,
				int limit) {
			this.nodeTypes = nodeTypes;
			this.query = query;
			this.projectId = projectId;
			this.limit = limit;
			return List.of(node());
		}

		private SourceOntologyNode node() {
			return new SourceOntologyNode(
				"type:management:UserService",
				SourceOntologyNodeType.JAVA_TYPE,
				"UserService",
				"management",
				"src/main/java/UserService.java",
				"com.example.UserService",
				"",
				"",
				"",
				"",
				"",
				"",
				"",
				"");
		}

	}

}
