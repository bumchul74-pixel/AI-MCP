package com.hanwha.mcp.domain.model;

import java.util.List;

public record MyBatisMapperGeneration(
		String databaseProductName,
		String databaseProductVersion,
		String schemaName,
		String tableName,
		String domainObjectName,
		String dtoClassName,
		String mapperInterfaceName,
		String mapperNamespace,
		List<MyBatisColumnMapping> columns,
		List<GeneratedFileArtifact> files,
		List<String> warnings) {

	public MyBatisMapperGeneration {
		validateText(tableName, "tableName");
		validateText(domainObjectName, "domainObjectName");
		validateText(dtoClassName, "dtoClassName");
		validateText(mapperInterfaceName, "mapperInterfaceName");
		validateText(mapperNamespace, "mapperNamespace");
		columns = List.copyOf(columns);
		files = List.copyOf(files);
		warnings = List.copyOf(warnings);
	}

	private static void validateText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
	}

}