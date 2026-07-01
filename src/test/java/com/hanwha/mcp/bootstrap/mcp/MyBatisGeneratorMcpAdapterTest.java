package com.hanwha.mcp.bootstrap.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.sql.Types;
import java.util.List;

import com.hanwha.mcp.application.dto.MyBatisGenerationOperation;
import com.hanwha.mcp.application.dto.MyBatisMapperGenerationQuery;
import com.hanwha.mcp.application.usecase.GenerateMyBatisMapperUseCase;
import com.hanwha.mcp.common.exception.InvalidMcpInputException;
import com.hanwha.mcp.domain.model.DatabaseColumnMetadata;
import com.hanwha.mcp.domain.model.DatabaseForeignKeyMetadata;
import com.hanwha.mcp.domain.model.DatabaseIndexMetadata;
import com.hanwha.mcp.domain.model.DatabaseTableMetadata;
import com.hanwha.mcp.domain.model.DatabaseTableSummary;
import com.hanwha.mcp.domain.model.GeneratedFileArtifact;
import com.hanwha.mcp.domain.model.MyBatisColumnMapping;
import com.hanwha.mcp.domain.model.MyBatisMapperGeneration;
import com.hanwha.mcp.domain.repository.DatabaseSchemaInspector;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

class MyBatisGeneratorMcpAdapterTest {

	@Test
	void generateMyBatisMapperReturnsArtifactsAndCapturesQuery() {
		var registry = new SimpleMeterRegistry();
		var useCase = new CapturingGenerateMyBatisMapperUseCase();
		var adapter = adapter(useCase, registry);

		var response = adapter.generateMyBatisMapper(
			"public.users",
			null,
			"User",
			"com.acme.app",
			null,
			null,
			"SELECT,INSERT");

		assertThat(response.tableName()).isEqualTo("users");
		assertThat(response.files()).extracting("role").containsExactly("dto");
		assertThat(useCase.query.tableName()).isEqualTo("users");
		assertThat(useCase.query.schemaName()).isEqualTo("public");
		assertThat(useCase.query.operations()).containsExactlyInAnyOrder(
			MyBatisGenerationOperation.SELECT_BY_ID,
			MyBatisGenerationOperation.SELECT_ALL,
			MyBatisGenerationOperation.INSERT);
		assertThat(registry.counter("mcp.tool.invocation.count", "tool", "generate_mybatis_mapper").count())
			.isEqualTo(1.0);
	}

	@Test
	void databaseMetadataToolsReturnReadOnlySchemaDetails() {
		var registry = new SimpleMeterRegistry();
		var adapter = adapter(new CapturingGenerateMyBatisMapperUseCase(), registry);

		var tables = adapter.listDatabaseTables("public");
		var search = adapter.searchDatabaseTables("user", "public");
		var columns = adapter.describeDatabaseTableColumns("public.users", null);
		var foreignKeys = adapter.describeDatabaseForeignKeys("users", "public");
		var indexes = adapter.describeDatabaseIndexes("users", "public");
		var comments = adapter.describeDatabaseComments("users", "public");

		assertThat(tables.tableCount()).isEqualTo(2);
		assertThat(tables.tables()).extracting("tableName").containsExactly("users", "customer_order");
		assertThat(search.tables()).extracting("tableName").containsExactly("users");
		assertThat(columns.primaryKeyColumns()).containsExactly("id");
		assertThat(columns.columns()).extracting("columnName").containsExactly("id", "email", "organization_id");
		assertThat(columns.columns().get(1).nullable()).isFalse();
		assertThat(columns.columns().get(1).jdbcTypeName()).isEqualTo("varchar");
		assertThat(foreignKeys.foreignKeys()).singleElement()
			.extracting("foreignKeyName")
			.isEqualTo("fk_users_organization");
		assertThat(indexes.indexes()).singleElement()
			.satisfies(index -> {
				assertThat(index.indexName()).isEqualTo("users_email_idx");
				assertThat(index.indexQualifier()).isEmpty();
				assertThat(index.filterCondition()).isEmpty();
				assertThat(index.unique()).isTrue();
			});
		assertThat(comments.tableRemarks()).isEqualTo("Application users");
		assertThat(comments.columns()).extracting("columnName", "remarks")
			.contains(tuple("email", "Login email"));
		assertThat(registry.counter("mcp.tool.invocation.count", "tool", "list_database_tables").count())
			.isEqualTo(1.0);
	}

	@Test
	void databaseMetadataToolsReturnEmptyStringsForMissingOptionalText() {
		var adapter = adapter(
			new CapturingGenerateMyBatisMapperUseCase(),
			new NullableMetadataDatabaseSchemaInspector(),
			new SimpleMeterRegistry());

		var tables = adapter.listDatabaseTables(null);
		var columns = adapter.describeDatabaseTableColumns("users", null);
		var comments = adapter.describeDatabaseComments("users", null);

		assertThat(tables.schemaName()).isEmpty();
		assertThat(tables.tables()).singleElement()
			.satisfies(table -> {
				assertThat(table.catalogName()).isEmpty();
				assertThat(table.schemaName()).isEmpty();
				assertThat(table.remarks()).isEmpty();
			});
		assertThat(columns.catalogName()).isEmpty();
		assertThat(columns.schemaName()).isEmpty();
		assertThat(columns.tableRemarks()).isEmpty();
		assertThat(columns.columns()).singleElement()
			.satisfies(column -> {
				assertThat(column.defaultValue()).isEmpty();
				assertThat(column.remarks()).isEmpty();
			});
		assertThat(comments.tableRemarks()).isEmpty();
		assertThat(comments.columns()).singleElement()
			.extracting("remarks")
			.isEqualTo("");
	}

	@Test
	void generateMyBatisMapperRejectsBlankTableName() {
		var adapter = adapter(new CapturingGenerateMyBatisMapperUseCase(), new SimpleMeterRegistry());

		assertThatThrownBy(() -> adapter.generateMyBatisMapper(" ", null, null, null, null, null, null))
			.isInstanceOf(InvalidMcpInputException.class)
			.hasMessageContaining("tableName must not be blank");
	}

	private MyBatisGeneratorMcpAdapter adapter(
			GenerateMyBatisMapperUseCase useCase,
			SimpleMeterRegistry registry) {
		return adapter(useCase, new StubDatabaseSchemaInspector(), registry);
	}

	private MyBatisGeneratorMcpAdapter adapter(
			GenerateMyBatisMapperUseCase useCase,
			DatabaseSchemaInspector databaseSchemaInspector,
			SimpleMeterRegistry registry) {
		return new MyBatisGeneratorMcpAdapter(
			useCase,
			databaseSchemaInspector,
			Validation.buildDefaultValidatorFactory().getValidator(),
			registry);
	}

	private static class CapturingGenerateMyBatisMapperUseCase implements GenerateMyBatisMapperUseCase {

		private MyBatisMapperGenerationQuery query;

		@Override
		public MyBatisMapperGeneration generate(MyBatisMapperGenerationQuery query) {
			this.query = query;
			return new MyBatisMapperGeneration(
				"PostgreSQL",
				"16",
				query.schemaName(),
				query.tableName(),
				query.domainObjectName() == null ? "User" : query.domainObjectName(),
				"UserDto",
				"UserMapper",
				"com.acme.app.mapper.UserMapper",
				List.of(new MyBatisColumnMapping("id", "id", "BIGINT", "Long", null, false, true, true, 1)),
				List.of(new GeneratedFileArtifact("dto", "UserDto.java", "src/main/java/com/acme/app/dto/UserDto.java", "java", "class UserDto {}")),
				List.of());
		}

	}

	private static class NullableMetadataDatabaseSchemaInspector implements DatabaseSchemaInspector {

		@Override
		public DatabaseTableMetadata inspectTable(String schemaName, String tableName) {
			return new DatabaseTableMetadata(
				null,
				null,
				"users",
				null,
				null,
				null,
				null,
				List.of(new DatabaseColumnMetadata("id", Types.BIGINT, null, false, true, false, 1, null, null, null, null)));
		}

		@Override
		public List<DatabaseTableSummary> listTables(String schemaName) {
			return List.of(new DatabaseTableSummary(null, null, "users", null, null));
		}

	}

	private static class StubDatabaseSchemaInspector implements DatabaseSchemaInspector {

		@Override
		public DatabaseTableMetadata inspectTable(String schemaName, String tableName) {
			return usersTable();
		}

		@Override
		public List<DatabaseTableSummary> listTables(String schemaName) {
			return List.of(
				new DatabaseTableSummary("sampledb", schemaName, "users", "TABLE", "Application users"),
				new DatabaseTableSummary("sampledb", schemaName, "customer_order", "TABLE", "Customer orders"));
		}

		@Override
		public List<DatabaseTableSummary> searchTables(String schemaName, String keyword) {
			return List.of(new DatabaseTableSummary("sampledb", schemaName, "users", "TABLE", "Application users"));
		}

		@Override
		public List<DatabaseForeignKeyMetadata> inspectForeignKeys(String schemaName, String tableName) {
			return List.of(new DatabaseForeignKeyMetadata(
				"IMPORTED",
				"fk_users_organization",
				"organization_pkey",
				"sampledb",
				"public",
				"organization",
				"id",
				"sampledb",
				"public",
				"users",
				"organization_id",
				1,
				"NO_ACTION",
				"CASCADE",
				"NOT_DEFERRABLE"));
		}

		@Override
		public List<DatabaseIndexMetadata> inspectIndexes(String schemaName, String tableName) {
			return List.of(new DatabaseIndexMetadata(null, "users_email_idx", "email", 1, true, "OTHER", "ASC", 10L, 1L, null));
		}

		private DatabaseTableMetadata usersTable() {
			return new DatabaseTableMetadata(
				"sampledb",
				"public",
				"users",
				"TABLE",
				"Application users",
				"PostgreSQL",
				"16",
				List.of(
					column("id", Types.BIGINT, "int8", false, true, true, 1, "User ID"),
					column("email", Types.VARCHAR, "varchar", false, false, false, 2, "Login email"),
					column("organization_id", Types.BIGINT, "int8", true, false, false, 3, "Organization ID")));
		}

		private DatabaseColumnMetadata column(
				String name,
				int jdbcType,
				String jdbcTypeName,
				boolean nullable,
				boolean primaryKey,
				boolean autoIncrement,
				int ordinalPosition,
				String remarks) {
			return new DatabaseColumnMetadata(
				name,
				jdbcType,
				jdbcTypeName,
				nullable,
				primaryKey,
				autoIncrement,
				ordinalPosition,
				null,
				null,
				null,
				remarks);
		}

	}

}