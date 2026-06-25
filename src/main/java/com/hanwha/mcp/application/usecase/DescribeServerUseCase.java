package com.hanwha.mcp.application.usecase;

import com.hanwha.mcp.application.dto.ServerInfoQuery;
import com.hanwha.mcp.application.dto.ServerInfoResponse;

public interface DescribeServerUseCase {

	ServerInfoResponse describe(ServerInfoQuery query);

}