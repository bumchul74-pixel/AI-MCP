package com.hanwha.mcp.application.usecase;

import com.hanwha.mcp.application.dto.MyBatisMapperGenerationQuery;
import com.hanwha.mcp.domain.model.MyBatisMapperGeneration;

public interface GenerateMyBatisMapperUseCase {

	MyBatisMapperGeneration generate(MyBatisMapperGenerationQuery query);

}