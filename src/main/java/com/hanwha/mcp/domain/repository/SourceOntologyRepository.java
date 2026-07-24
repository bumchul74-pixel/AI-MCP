package com.hanwha.mcp.domain.repository;

import java.util.List;
import java.util.Set;

import com.hanwha.mcp.domain.model.SourceOntologyNode;
import com.hanwha.mcp.domain.model.SourceOntologyNodeType;

public interface SourceOntologyRepository {

	List<SourceOntologyNode> search(
			Set<SourceOntologyNodeType> nodeTypes,
			String query,
			String projectId,
			int limit);

}
