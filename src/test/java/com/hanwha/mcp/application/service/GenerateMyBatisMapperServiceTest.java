package com.hanwha.mcp.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Types;
import java.util.List;

import com.hanwha.mcp.application.dto.MyBatisGeneratorDefaults;
import com.hanwha.mcp.application.dto.MyBatisMapperGenerationQuery;
import com.hanwha.mcp.domain.model.DatabaseColumnMetadata;
import com.hanwha.mcp.domain.model.DatabaseTableMetadata;
import com.hanwha.mcp.domain.model.GeneratedFileArtifact;
import com.hanwha.mcp.domain.repository.DatabaseSchemaInspector;
import org.junit.jupiter.api.Test;

class GenerateMyBatisMapperServiceTest {

	@Test
	void generateCreatesDtoMapperInterfaceAndXmlFromTableMetadata() {
		var service = service(table(
			"public",
			"customer_order",
			List.of(
				column("id", Types.BIGINT, "int8", false, true, true, 1),
				column("user_name", Types.VARCHAR, "varchar", false, false, false, 2),
				column("created_at", Types.TIMESTAMP, "timestamp", false, false, false, 3),
				column("amount", Types.NUMERIC, "numeric", true, false, false, 4))));

		var response = service.generate(MyBatisMapperGenerationQuery.from(
			"customer_order",
			"public",
			"CustomerOrder",
			"com.acme.sales",
			null,
			null,
			"CRUD"));

		assertThat(response.schemaName()).isEqualTo("public");
		assertThat(response.dtoClassName()).isEqualTo("CustomerOrderDto");
		assertThat(response.mapperInterfaceName()).isEqualTo("CustomerOrderMapper");
		assertThat(response.mapperNamespace()).isEqualTo("com.acme.sales.mapper.CustomerOrderMapper");
		assertThat(response.columns()).extracting("propertyName").containsExactly("id", "userName", "createdAt", "amount");
		assertThat(file(response.files(), "dto").content())
			.contains("package com.acme.sales.dto;")
			.contains("import java.math.BigDecimal;")
			.contains("import java.time.LocalDateTime;")
			.contains("private String userName;");
		assertThat(file(response.files(), "mapper-interface").content())
			.contains("Optional<CustomerOrderDto> findById(Long id);")
			.contains("int insert(CustomerOrderDto customerOrder);")
			.contains("int update(CustomerOrderDto customerOrder);");
		assertThat(file(response.files(), "mapper-xml").content())
			.contains("FROM public.customer_order")
			.contains("useGeneratedKeys=\"true\" keyProperty=\"id\"")
			.contains("user_name = #{userName,jdbcType=VARCHAR}")
			.contains("<id column=\"id\" property=\"id\" jdbcType=\"BIGINT\" />");
	}

	@Test
	void generateSkipsPrimaryKeyOperationsWhenTableHasNoPrimaryKey() {
		var service = service(table(
			null,
			"audit_log",
			List.of(
				column("event_name", Types.VARCHAR, "varchar", false, false, false, 1),
				column("created_at", Types.TIMESTAMP, "timestamp", false, false, false, 2))));

		var response = service.generate(MyBatisMapperGenerationQuery.from(
			"audit_log",
			null,
			null,
			"com.acme.audit",
			null,
			null,
			"CRUD"));

		assertThat(response.domainObjectName()).isEqualTo("AuditLog");
		assertThat(response.warnings()).contains(
			"SELECT_BY_ID skipped because table has no primary key.",
			"UPDATE_BY_ID skipped because table has no primary key.",
			"DELETE_BY_ID skipped because table has no primary key.");
		assertThat(file(response.files(), "mapper-interface").content())
			.contains("List<AuditLogDto> findAll();")
			.contains("int insert(AuditLogDto auditLog);")
			.doesNotContain("findById")
			.doesNotContain("deleteById")
			.doesNotContain("int update");
	}

	private GenerateMyBatisMapperService service(DatabaseTableMetadata table) {
		return new GenerateMyBatisMapperService(new StubDatabaseSchemaInspector(table), new MyBatisGeneratorDefaults("com.example.app", null));
	}

	private DatabaseTableMetadata table(String schemaName, String tableName, List<DatabaseColumnMetadata> columns) {
		return new DatabaseTableMetadata(
			"sampledb",
			schemaName,
			tableName,
			"TABLE",
			null,
			"PostgreSQL",
			"16",
			columns);
	}

	private DatabaseColumnMetadata column(
			String name,
			int jdbcType,
			String jdbcTypeName,
			boolean nullable,
			boolean primaryKey,
			boolean autoIncrement,
			int ordinal) {
		return new DatabaseColumnMetadata(
			name,
			jdbcType,
			jdbcTypeName,
			nullable,
			primaryKey,
			autoIncrement,
			ordinal,
			null,
			null,
			null,
			null);
	}

	private GeneratedFileArtifact file(List<GeneratedFileArtifact> files, String role) {
		return files.stream()
			.filter(file -> file.role().equals(role))
			.findFirst()
			.orElseThrow();
	}

	private record StubDatabaseSchemaInspector(DatabaseTableMetadata table) implements DatabaseSchemaInspector {

		@Override
		public DatabaseTableMetadata inspectTable(String schemaName, String tableName) {
			return this.table;
		}

	}

}