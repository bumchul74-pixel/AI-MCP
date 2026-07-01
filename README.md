# AI MCP Server

Production-ready Spring Boot MCP server scaffold for Hanwha.

## Stack

- Java 21
- Spring Boot 4.0.6
- Spring AI MCP 2.0.0
- Gradle Wrapper
- JUnit 5
- Micrometer and Spring Boot Actuator

## Architecture

The project follows the repository ADRs and keeps framework concerns outside business logic.

- `bootstrap`: Spring Boot entrypoint, configuration, and inbound MCP adapters
- `application`: use cases, application services, and DTOs
- `domain`: framework-free models and ports
- `infrastructure`: outbound adapters
- `common`: shared error contracts

Dependency direction:

```text
bootstrap -> application
bootstrap -> infrastructure
application -> domain
infrastructure -> domain
```

## MCP Contracts

### Tool: `get_server_info`

Purpose: returns read-only metadata and capabilities for this MCP server.

Input:

```json
{
  "detailLevel": "BASIC"
}
```

Allowed `detailLevel` values are `BASIC` and `EXTENDED`. The value is optional and defaults to `BASIC`.

Output:

```json
{
  "name": "ai-mcp-server",
  "version": "0.0.1",
  "description": "MCP server for analyzing Gradle/Spring Boot project structure.",
  "detailLevel": "BASIC",
  "capabilities": ["mcp-tools", "mcp-resources", "mcp-prompts"],
  "generatedAt": "2026-06-19T00:00:00Z"
}
```

Error cases:

- `detailLevel` is not `BASIC` or `EXTENDED`
- `detailLevel` exceeds 16 characters

### Tool: `analyze_project_structure`

Purpose: analyzes a local project directory for Java package structure, Gradle build
metadata, Java version, Spring Boot version, dependencies, and test structure.

Input:

```json
{
  "projectPath": "D:/workspace/AI-MCP"
}
```

`projectPath` must point to a readable project directory. The analyzer supports
`build.gradle.kts` and `build.gradle`, preferring `build.gradle.kts` when both exist.
The tool performs static analysis only and does not execute Gradle.

Output:

```json
{
  "projectPath": "D:/workspace/AI-MCP",
  "projectName": "ai-mcp",
  "packages": [
    {
      "sourceSet": "main",
      "packageName": "com.hanwha.mcp.bootstrap",
      "sourceFileCount": 4
    }
  ],
  "build": {
    "present": true,
    "fileName": "build.gradle",
    "javaVersion": "21",
    "springBootVersion": "4.0.6",
    "dependencies": [
      {
        "configuration": "implementation",
        "notation": "org.springframework.boot:spring-boot-starter-web"
      }
    ]
  },
  "testStructure": {
    "present": true,
    "directories": ["src/test/java"],
    "testSourceFileCount": 4,
    "testClassCount": 4,
    "frameworks": ["Spring Boot Test", "JUnit 5", "AssertJ"]
  },
  "warnings": []
}
```

Error cases:

- `projectPath` is blank, too long, or contains control characters
- `projectPath` does not exist
- `projectPath` is not a readable directory


### Tool: `generate_mybatis_mapper`

Purpose: inspects a configured JDBC database table and generates MyBatis DTO,
mapper interface, and mapper XML artifacts from the table columns and primary key.
The tool returns generated file contents; it does not write files to disk.

Input:

```json
{
  "tableName": "public.users",
  "schemaName": null,
  "domainObjectName": "User",
  "basePackage": "com.example.app",
  "dtoPackage": null,
  "mapperPackage": null,
  "operations": "CRUD"
}
```

`tableName` is required. `schemaName` may be passed separately or as `schema.table`.
`domainObjectName` defaults to the table name in PascalCase. `operations` defaults
to `CRUD` and supports `SELECT`, `SELECT_BY_ID`, `SELECT_ALL`, `INSERT`, `UPDATE`,
`UPDATE_BY_ID`, `DELETE`, and `DELETE_BY_ID`.

Output shape:

```json
{
  "databaseProductName": "PostgreSQL",
  "schemaName": "public",
  "tableName": "users",
  "dtoClassName": "UserDto",
  "mapperInterfaceName": "UserMapper",
  "mapperNamespace": "com.example.app.mapper.UserMapper",
  "columns": [
    {
      "columnName": "id",
      "propertyName": "id",
      "jdbcType": "BIGINT",
      "javaType": "Long",
      "primaryKey": true,
      "autoIncrement": true
    }
  ],
  "files": [
    {
      "role": "dto",
      "suggestedPath": "src/main/java/com/example/app/dto/UserDto.java",
      "language": "java",
      "content": "..."
    }
  ],
  "warnings": []
}
```

Error cases:

- `mcp.mybatis.datasource.url` is not configured
- The configured JDBC driver class is not available
- The requested table cannot be found
- `tableName`, package names, or operation names are invalid


### Database metadata tools

These tools inspect the configured JDBC database without modifying it. `schemaName`
is optional for all table-scoped tools, and table names may be passed as
`schema.table` when `schemaName` is omitted.

- `list_database_tables`: lists tables and views, optionally within one schema.
- `search_database_tables`: searches related tables by keyword across table names,
  schema names, table remarks, column names, column remarks, and column type names.
- `describe_database_table_columns`: returns columns, JDBC type code/name, primary-key
  flag, nullable flag, auto-increment flag, ordinal position, size, default value,
  and remarks.
- `describe_database_foreign_keys`: returns imported and exported FK relationships.
- `describe_database_indexes`: returns index names, indexed columns, uniqueness,
  ordinal position, index type, sort direction, cardinality, pages, and filter condition.
- `describe_database_comments`: returns table remarks and column remarks.

Example table lookup input:

```json
{
  "tableName": "public.users",
  "schemaName": null
}
```
### Resource: `server://info`

Purpose: exposes read-only JSON server metadata.

Access pattern:

```text
server://info
```

Example response:

```json
{
  "name": "ai-mcp-server",
  "version": "0.0.1",
  "description": "MCP server for analyzing Gradle/Spring Boot project structure.",
  "detailLevel": "EXTENDED",
  "capabilities": ["mcp-tools", "mcp-resources", "mcp-prompts", "actuator", "metrics"]
}
```

### Prompt: `summarize_server_status`

Intent: asks an AI client to summarize server status using `server://info` as the primary source.

Example usage:

```json
{
  "audience": "operations team"
}
```

## Configuration

Main configuration lives in `src/main/resources/application.yml`.

Important properties:

```yaml
spring.ai.mcp.server.protocol: STREAMABLE
spring.ai.mcp.server.streamable-http.mcp-endpoint: /mcp
mcp.server.metadata.name: ai-mcp-server
mcp.server.metadata.version: 0.0.1
mcp.server.metadata.description: MCP server for analyzing Gradle/Spring Boot projects and generating MyBatis artifacts from database metadata.
mcp.mybatis.datasource.url: ${MCP_MYBATIS_DB_URL:}
mcp.mybatis.datasource.username: ${MCP_MYBATIS_DB_USERNAME:}
mcp.mybatis.datasource.password: ${MCP_MYBATIS_DB_PASSWORD:}
mcp.mybatis.datasource.driver-class-name: ${MCP_MYBATIS_DB_DRIVER:org.postgresql.Driver}
mcp.mybatis.generator.base-package: ${MCP_MYBATIS_BASE_PACKAGE:com.example.app}
mcp.mybatis.generator.default-schema: ${MCP_MYBATIS_DEFAULT_SCHEMA:}
```

Actuator exposes `health`, `info`, and `metrics`.

## Build and Test

```bash
./gradlew clean test
```

On Windows:

```powershell
$env:JAVA_OPTS='-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT'
.\gradlew.bat clean test
```

If VS Code reports a Spring IO certificate error such as `PKIX path building failed`
while fetching Spring project metadata, reload the VS Code window after opening this
workspace. The workspace config in `.vscode/settings.json` passes
`-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT` to the Java and Spring Boot language
servers so they can use the Windows trusted root certificate store.

## Run

```bash
./gradlew bootRun
```

The MCP streamable HTTP endpoint is available at `/mcp`.