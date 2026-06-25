# AI MCP Server

Production-ready Spring Boot MCP server scaffold for Hanwha.

## Stack

- Java 21
- Spring Boot 3.5.15
- Spring AI MCP 1.1.8
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
mcp.server.metadata.description: MCP server for analyzing Gradle/Spring Boot project structure.
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