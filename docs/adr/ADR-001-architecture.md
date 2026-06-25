# ADR-001: Architecture Style

## Status
Accepted

## Context
This project is a Spring Boot based MCP Server.
It must remain maintainable, testable, and extensible.

## Decision
Use Hexagonal Architecture.

## Rules
- Domain must not depend on Spring.
- Application layer orchestrates use cases.
- MCP Tools are inbound adapters.
- External APIs, databases, and file systems are outbound adapters.
- Infrastructure depends on domain, not the reverse.

## Consequences
- More initial structure is required.
- Business logic remains isolated.
- Testing becomes easier.