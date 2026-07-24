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

### Source ontology tool

`search_source_ontology` performs read-only searches against the Java source graph
stored in Neo4j. It supports these node types:

- `JAVA_TYPE`: Java classes, interfaces, records, enums, and external types.
- `METHOD`: declared and resolved method nodes.
- `FIELD`: Java fields.
- `API_ENDPOINT`: HTTP method and endpoint path nodes.
- `SQL_STATEMENT`: MyBatis statement identifiers, namespaces, SQL, and operations.

All arguments are optional. `nodeTypes` defaults to all five types, `limit` defaults
to `50`, and the maximum limit is `100`. `query` is matched case-insensitively against
names, FQNs, file paths, signatures, endpoint paths, namespaces, statement IDs, and
SQL. `projectId` is an exact filter.

Example:

```json
{
  "nodeTypes": ["JAVA_TYPE", "METHOD", "API_ENDPOINT"],
  "query": "user",
  "projectId": "management",
  "limit": 25
}
```

The tool never creates, updates, or deletes Neo4j data. Configure a Neo4j account
with read-only privileges for production use. If Neo4j is unavailable, AI-MCP can
still start; only invocations of this tool fail.

### Secure coding tools

The server connects to the configured `secure-coding-mcp` Streamable HTTP endpoint
and re-exposes its Semgrep CE tools through this server's `/mcp` endpoint. Tool
input and output schemas are discovered from the downstream MCP server.

- `scan_project`: scans a Java project below the downstream server's workspace.
- `scan_file`: scans one Java file below the downstream server's workspace.
- `scan_source`: scans UTF-8 Java source content without a shared filesystem.
- `list_rules`: lists the Semgrep rule sets available on the downstream server.

Example `scan_project` arguments:

```yaml
path: my-java-project
ruleSets:
  - java-security
```

`ruleSets` is optional. Paths used by `scan_project` and `scan_file` are resolved by
`secure-coding-mcp`, so those tools still require a mounted source workspace.

Example `scan_source` arguments:

```json
{
  "fileName": "UserService.java",
  "source": "public class UserService {}",
  "ruleSets": ["java-security"]
}
```

`scan_source` forwards the source as an MCP tool argument. AI-MCP does not persist or
log the source. The downstream server creates an isolated temporary file for Semgrep
and deletes it immediately after the scan.

### EasyOCR tools

The server also connects to the configured EasyOCR Streamable HTTP endpoint and
re-exposes its OCR tools through the same AI-MCP `/mcp` endpoint.

- `ocr_image_file`: extracts Korean and English text from an image file visible to
  the EasyOCR server and allowed by its `EASYOCR_ALLOWED_DIRS` setting.
- `ocr_image_base64`: extracts text from a Base64 image or image data URL. Use this
  tool when AI-MCP and EasyOCR do not share a filesystem.

Example Base64 tool arguments:

```json
{
  "image_base64": "data:image/png;base64,..."
}
```

The downstream response contains the combined `text`, detected `regions`, region
count, configured languages, and source. Invalid Base64 data, unsupported image
types, files outside the allowed directories, and images larger than 20 MB are
returned as tool errors.

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
spring.ai.mcp.server.expose-mcp-client-tools: true
spring.ai.mcp.client.enabled: ${DOWNSTREAM_MCP_ENABLED:${SECURE_CODING_MCP_ENABLED:true}}
spring.ai.mcp.client.streamable-http.connections.secure-coding-mcp.url: ${SECURE_CODING_MCP_URL:http://localhost:18090}
spring.ai.mcp.client.streamable-http.connections.secure-coding-mcp.endpoint: ${SECURE_CODING_MCP_ENDPOINT:/mcp}
spring.ai.mcp.client.streamable-http.connections.easyocr-mcp.url: ${EASYOCR_MCP_URL:http://localhost:8001}
spring.ai.mcp.client.streamable-http.connections.easyocr-mcp.endpoint: ${EASYOCR_MCP_ENDPOINT:/ocr}
mcp.server.metadata.name: ai-mcp-server
mcp.server.metadata.version: 0.0.1
mcp.server.metadata.description: MCP server for analyzing Java projects, querying source ontology, and generating MyBatis artifacts.
mcp.ontology.neo4j.uri: ${NEO4J_URI:bolt://localhost:7687}
mcp.ontology.neo4j.username: ${NEO4J_USERNAME:neo4j}
mcp.ontology.neo4j.password: ${NEO4J_PASSWORD:}
mcp.ontology.neo4j.database: ${NEO4J_DATABASE:neo4j}
mcp.mybatis.datasource.url: ${MCP_MYBATIS_DB_URL:}
mcp.mybatis.datasource.username: ${MCP_MYBATIS_DB_USERNAME:}
mcp.mybatis.datasource.password: ${MCP_MYBATIS_DB_PASSWORD:}
mcp.mybatis.datasource.driver-class-name: ${MCP_MYBATIS_DB_DRIVER:org.postgresql.Driver}
mcp.mybatis.generator.base-package: ${MCP_MYBATIS_BASE_PACKAGE:com.example.app}
mcp.mybatis.generator.default-schema: ${MCP_MYBATIS_DEFAULT_SCHEMA:}
```

Actuator exposes `health`, `info`, and `metrics`.

When this application runs directly on the host, the default downstream URL uses
the published Docker port `http://localhost:18090`. When both applications run in
the same Docker Compose network, configure the AI MCP service with:

```yaml
environment:
  SECURE_CODING_MCP_URL: http://secure-coding-mcp:8080
```

The downstream MCP client is enabled by default. Both SecureCoding MCP and EasyOCR
MCP must be reachable while AI-MCP starts. Set `DOWNSTREAM_MCP_ENABLED=false` when
AI-MCP must run without downstream tools. `SECURE_CODING_MCP_ENABLED` remains as a
backward-compatible fallback for the global client switch.

`SECURE_CODING_MCP_REQUEST_TIMEOUT` defaults to `60s`, and
`SECURE_CODING_MCP_ENDPOINT` defaults to `/mcp`. `EASYOCR_MCP_URL` defaults to
`http://localhost:8001`, and `EASYOCR_MCP_ENDPOINT` defaults to `/ocr`.

## Build and Test

```bash
./gradlew clean test
```

On Windows:

```powershell
$env:JAVA_OPTS='-Djavax.net.ssl.trustStoreType=WINDOWS-ROOT'
.\gradlew.bat clean test
```

To verify the `legacy-interface` MCP server running in Docker, start the container
and run the dedicated integration-test task:

```powershell
.\gradlew.bat legacyInterfaceMcpTest
```

The test initializes a Streamable HTTP MCP session, verifies `scan_project`, `scan_file`, `scan_source`, and `list_rules` are exposed,
then invokes both `scan_source` and the read-only `list_rules` tool. The default endpoint is `http://localhost:18090/mcp`. Override it when needed:

```powershell
$env:LEGACY_INTERFACE_MCP_URL='http://localhost:18090/mcp'
.\gradlew.bat legacyInterfaceMcpTest
```

The regular `test` task excludes this Docker-dependent integration test.

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
