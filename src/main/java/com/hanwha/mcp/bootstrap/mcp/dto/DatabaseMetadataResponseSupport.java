package com.hanwha.mcp.bootstrap.mcp.dto;

final class DatabaseMetadataResponseSupport {

	private DatabaseMetadataResponseSupport() {
	}

	static String text(String value) {
		return value == null ? "" : value;
	}

}
