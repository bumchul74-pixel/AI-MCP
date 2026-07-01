package com.hanwha.mcp.bootstrap.mcp.dto;

import static com.hanwha.mcp.bootstrap.mcp.dto.DatabaseMetadataResponseSupport.text;

import java.util.List;

import com.hanwha.mcp.domain.model.DatabaseForeignKeyMetadata;

public record DatabaseForeignKeyToolResponse(
		String schemaName,
		String tableName,
		int foreignKeyCount,
		List<ForeignKeyResponse> foreignKeys) {

	public static DatabaseForeignKeyToolResponse from(
			String schemaName,
			String tableName,
			List<DatabaseForeignKeyMetadata> foreignKeys) {
		return new DatabaseForeignKeyToolResponse(
			text(schemaName),
			text(tableName),
			foreignKeys.size(),
			foreignKeys.stream().map(ForeignKeyResponse::from).toList());
	}

	public record ForeignKeyResponse(
			String direction,
			String foreignKeyName,
			String primaryKeyName,
			String primaryKeyCatalogName,
			String primaryKeySchemaName,
			String primaryKeyTableName,
			String primaryKeyColumnName,
			String foreignKeyCatalogName,
			String foreignKeySchemaName,
			String foreignKeyTableName,
			String foreignKeyColumnName,
			int keySequence,
			String updateRule,
			String deleteRule,
			String deferrability) {

		static ForeignKeyResponse from(DatabaseForeignKeyMetadata metadata) {
			return new ForeignKeyResponse(
				text(metadata.direction()),
				text(metadata.foreignKeyName()),
				text(metadata.primaryKeyName()),
				text(metadata.primaryKeyCatalogName()),
				text(metadata.primaryKeySchemaName()),
				text(metadata.primaryKeyTableName()),
				text(metadata.primaryKeyColumnName()),
				text(metadata.foreignKeyCatalogName()),
				text(metadata.foreignKeySchemaName()),
				text(metadata.foreignKeyTableName()),
				text(metadata.foreignKeyColumnName()),
				metadata.keySequence(),
				text(metadata.updateRule()),
				text(metadata.deleteRule()),
				text(metadata.deferrability()));
		}

	}

}