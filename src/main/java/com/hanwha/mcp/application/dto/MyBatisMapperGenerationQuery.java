package com.hanwha.mcp.application.dto;

import java.util.Set;

public record MyBatisMapperGenerationQuery(
		String tableName,
		String schemaName,
		String domainObjectName,
		String basePackage,
		String dtoPackage,
		String mapperPackage,
		Set<MyBatisGenerationOperation> operations) {

	public MyBatisMapperGenerationQuery {
		if (tableName == null || tableName.isBlank()) {
			throw new IllegalArgumentException("tableName must not be blank");
		}
		tableName = tableName.trim();
		if ((schemaName == null || schemaName.isBlank()) && tableName.indexOf('.') > 0) {
			var parts = tableName.split("\\.", 2);
			schemaName = parts[0];
			tableName = parts[1];
		}
		schemaName = normalize(schemaName);
		domainObjectName = normalize(domainObjectName);
		basePackage = normalize(basePackage);
		dtoPackage = normalize(dtoPackage);
		mapperPackage = normalize(mapperPackage);
		operations = operations == null || operations.isEmpty()
			? Set.copyOf(MyBatisGenerationOperation.parse("CRUD"))
			: Set.copyOf(operations);
	}

	public static MyBatisMapperGenerationQuery from(
			String tableName,
			String schemaName,
			String domainObjectName,
			String basePackage,
			String dtoPackage,
			String mapperPackage,
			String operations) {
		return new MyBatisMapperGenerationQuery(
			tableName,
			schemaName,
			domainObjectName,
			basePackage,
			dtoPackage,
			mapperPackage,
			MyBatisGenerationOperation.parse(operations));
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		value = value.trim();
		return value.isBlank() ? null : value;
	}

}