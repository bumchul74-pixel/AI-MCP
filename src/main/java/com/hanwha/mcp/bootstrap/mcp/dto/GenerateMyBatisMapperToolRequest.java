package com.hanwha.mcp.bootstrap.mcp.dto;

import com.hanwha.mcp.application.dto.MyBatisMapperGenerationQuery;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GenerateMyBatisMapperToolRequest(
		@NotBlank(message = "tableName must not be blank")
		@Size(max = 256, message = "tableName must be at most 256 characters")
		@Pattern(regexp = "^[^\\r\\n\\t\\u0000]+$", message = "tableName contains unsupported characters")
		String tableName,

		@Size(max = 128, message = "schemaName must be at most 128 characters")
		@Pattern(regexp = "^[^\\r\\n\\t\\u0000]*$", message = "schemaName contains unsupported characters")
		String schemaName,

		@Size(max = 128, message = "domainObjectName must be at most 128 characters")
		@Pattern(regexp = "^[A-Za-z_$][A-Za-z0-9_$]*$", message = "domainObjectName must be a Java simple class name")
		String domainObjectName,

		@Size(max = 256, message = "basePackage must be at most 256 characters")
		@Pattern(regexp = "^[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*$", message = "basePackage must be a Java package name")
		String basePackage,

		@Size(max = 256, message = "dtoPackage must be at most 256 characters")
		@Pattern(regexp = "^[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*$", message = "dtoPackage must be a Java package name")
		String dtoPackage,

		@Size(max = 256, message = "mapperPackage must be at most 256 characters")
		@Pattern(regexp = "^[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*$", message = "mapperPackage must be a Java package name")
		String mapperPackage,

		@Size(max = 128, message = "operations must be at most 128 characters")
		@Pattern(regexp = "^[A-Za-z_, ]*$", message = "operations may contain only letters, commas, and spaces")
		String operations) {

	public GenerateMyBatisMapperToolRequest {
		tableName = trim(tableName);
		schemaName = trim(schemaName);
		domainObjectName = trim(domainObjectName);
		basePackage = trim(basePackage);
		dtoPackage = trim(dtoPackage);
		mapperPackage = trim(mapperPackage);
		operations = trim(operations);
	}

	public MyBatisMapperGenerationQuery toQuery() {
		return MyBatisMapperGenerationQuery.from(
			this.tableName,
			this.schemaName,
			this.domainObjectName,
			this.basePackage,
			this.dtoPackage,
			this.mapperPackage,
			this.operations);
	}

	private static String trim(String value) {
		if (value == null) {
			return null;
		}
		value = value.trim();
		return value.isBlank() ? null : value;
	}

}