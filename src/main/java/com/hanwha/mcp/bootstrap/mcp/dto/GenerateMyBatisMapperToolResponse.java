package com.hanwha.mcp.bootstrap.mcp.dto;

import static com.hanwha.mcp.bootstrap.mcp.dto.DatabaseMetadataResponseSupport.text;

import java.util.List;

import com.hanwha.mcp.domain.model.GeneratedFileArtifact;
import com.hanwha.mcp.domain.model.MyBatisColumnMapping;
import com.hanwha.mcp.domain.model.MyBatisMapperGeneration;

public record GenerateMyBatisMapperToolResponse(
		String databaseProductName,
		String databaseProductVersion,
		String schemaName,
		String tableName,
		String domainObjectName,
		String dtoClassName,
		String mapperInterfaceName,
		String mapperNamespace,
		List<ColumnMappingResponse> columns,
		List<GeneratedFileResponse> files,
		List<String> warnings) {

	public static GenerateMyBatisMapperToolResponse from(MyBatisMapperGeneration generation) {
		return new GenerateMyBatisMapperToolResponse(
			text(generation.databaseProductName()),
			text(generation.databaseProductVersion()),
			text(generation.schemaName()),
			text(generation.tableName()),
			text(generation.domainObjectName()),
			text(generation.dtoClassName()),
			text(generation.mapperInterfaceName()),
			text(generation.mapperNamespace()),
			generation.columns().stream().map(ColumnMappingResponse::from).toList(),
			generation.files().stream().map(GeneratedFileResponse::from).toList(),
			generation.warnings());
	}

	public record ColumnMappingResponse(
			String columnName,
			String propertyName,
			String jdbcType,
			String javaType,
			boolean nullable,
			boolean primaryKey,
			boolean autoIncrement,
			int ordinalPosition) {

		static ColumnMappingResponse from(MyBatisColumnMapping mapping) {
			return new ColumnMappingResponse(
				text(mapping.columnName()),
				text(mapping.propertyName()),
				text(mapping.jdbcType()),
				text(mapping.javaType()),
				mapping.nullable(),
				mapping.primaryKey(),
				mapping.autoIncrement(),
				mapping.ordinalPosition());
		}

	}

	public record GeneratedFileResponse(
			String role,
			String fileName,
			String suggestedPath,
			String language,
			String content) {

		static GeneratedFileResponse from(GeneratedFileArtifact artifact) {
			return new GeneratedFileResponse(
				text(artifact.role()),
				text(artifact.fileName()),
				text(artifact.suggestedPath()),
				text(artifact.language()),
				text(artifact.content()));
		}

	}

}