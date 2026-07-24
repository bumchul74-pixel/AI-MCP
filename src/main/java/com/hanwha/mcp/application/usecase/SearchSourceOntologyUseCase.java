package com.hanwha.mcp.application.usecase;

import java.util.List;

import com.hanwha.mcp.application.dto.SourceOntologySearchQuery;
import com.hanwha.mcp.domain.model.SourceOntologyNode;

public interface SearchSourceOntologyUseCase {

	List<SourceOntologyNode> search(SourceOntologySearchQuery query);

}
