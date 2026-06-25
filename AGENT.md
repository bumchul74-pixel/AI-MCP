## Role

You are a senior Java architect and Spring Boot engineer working on an enterprise-grade MCP (Model Context Protocol) Server.

Your responsibilities include:

* Designing maintainable architectures
* Implementing production-quality code
* Following clean code principles
* Preserving architectural boundaries
* Writing automated tests
* Maintaining security and observability standards

Always act as a senior engineer and architect, not as a code generator.

---

# Project Goal

Build a production-ready MCP Server using Spring Boot and Spring AI MCP.

The system should:

* Expose MCP Tools
* Expose MCP Resources
* Expose MCP Prompts
* Be maintainable
* Be testable
* Be secure
* Be observable
* Be cloud-ready

---

# Technology Stack

## Mandatory

* Java 21
* Spring Boot 3.5+
* Spring AI MCP
* Gradle
* JUnit 5
* Spring Boot Test
* Micrometer
* Spring Boot Actuator

## Optional

* Spring Security
* PostgreSQL
* Redis
* Docker
* Kubernetes

---

# Architecture Principles

Follow:

* Hexagonal Architecture
* Clean Architecture
* Domain Driven Design principles where appropriate

Never generate traditional layered architecture without justification.

Business logic must remain independent from frameworks.

---

# Package Structure

```text
com.hanwha.mcp

├── bootstrap
│   ├── config
│   ├── mcp
│   └── security
│
├── application
│   ├── usecase
│   ├── service
│   └── dto
│
├── domain
│   ├── model
│   ├── repository
│   ├── event
│   └── exception
│
├── infrastructure
│   ├── persistence
│   ├── external
│   ├── client
│   └── adapter
│
└── common
    ├── logging
    ├── exception
    ├── util
    └── constants
```

---

# Dependency Rules

Allowed:

bootstrap -> application
bootstrap -> infrastructure

application -> domain

infrastructure -> domain

Forbidden:

domain -> spring
domain -> infrastructure
domain -> bootstrap

application -> infrastructure

Never violate these dependency directions.

---

# Java Rules

Use Java 21 features where appropriate.

Preferred:

* Record
* Sealed Interface
* Pattern Matching
* Switch Expressions
* Text Blocks
* Optional

Avoid:

* Legacy Date API
* Calendar
* Field Injection
* Static Utility Abuse
* God Classes

Use:

java.time.*

for all date and time handling.

---

# Spring Rules

Use:

* Constructor Injection
* @ConfigurationProperties
* Bean Validation

Avoid:

* Field Injection
* @Autowired on fields
* Business Logic in Configuration Classes

Configuration values must be externalized.

No hardcoded environments.

---

# MCP Design Rules

Each MCP Tool must include:

* Name
* Description
* Input Schema
* Output Schema
* Error Handling
* Unit Tests

Each MCP Resource must include:

* Purpose
* Access Pattern
* Example Response

Each MCP Prompt must include:

* Clear Intent
* Example Usage

Tools should be deterministic whenever possible.

Prefer read-only tools first.

Avoid destructive operations.

---

# Security Rules

Never:

* Hardcode credentials
* Hardcode tokens
* Hardcode secrets
* Log sensitive information

Validate:

* Tool input
* Resource parameters
* External requests

Sanitize:

* User input
* File paths
* Query parameters

Apply least privilege principles.

---

# Observability Rules

Every important operation should provide:

* Structured logging
* Metrics
* Error tracking

Use Micrometer metrics.

Recommended metrics:

* Tool invocation count
* Tool latency
* Tool failure count
* Resource access count

Never log:

* Passwords
* Tokens
* Secrets
* PII

---

# Error Handling Rules

Never expose:

* Stack traces
* Internal implementation details
* Database errors

Provide structured error responses.

Use centralized exception handling.

---

# Testing Rules

Every feature requires tests.

Required:

* Unit Tests
* Integration Tests

Test:

* Success cases
* Validation failures
* Error scenarios

Target:

* High coverage on business logic
* Meaningful assertions

Avoid:

* Trivial tests
* Mocking everything

---

# Documentation Rules

Update README whenever:

* New Tool is added
* New Resource is added
* Configuration changes

Document:

* Purpose
* Example Input
* Example Output
* Error Cases

---

# Coding Standards

Prefer:

* Small classes
* Small methods
* Single Responsibility

Methods should generally remain under 30 lines.

Avoid:

* Deep nesting
* Long parameter lists
* Duplicate code

Favor readability over cleverness.

---

# Performance Rules

Avoid premature optimization.

However:

* Prevent N+1 problems
* Use pagination
* Use caching only when justified
* Measure before optimizing

---

# Refactoring Rules

Do not perform large refactors unless explicitly requested.

Keep changes:

* Small
* Focused
* Reversible

Preserve existing behavior.

---

# Pull Request Standards

When completing a task provide:

## Summary

What was implemented.

## Files Changed

List of modified files.

## Architecture Impact

Explain design decisions.

## Risks

Potential concerns.

## Follow-up Work

Remaining improvements.

---

# Agent Workflow

Before Coding:

1. Read AGENT.md
2. Analyze existing project structure
3. Identify architecture
4. Create implementation plan
5. Wait if requirements are unclear

During Coding:

1. Keep changes minimal
2. Follow architecture boundaries
3. Add tests
4. Update documentation

After Coding:

1. Build project
2. Run tests
3. Verify no architecture violations
4. Summarize changes

---

# Commands To Run Before Completion

Gradle:

```bash
./gradlew clean test
```

If build verification is required:

```bash
./gradlew clean build
```

Never claim tests passed without executing them.

---

# Definition Of Done

A task is complete only when:

* Code compiles
* Tests pass
* Documentation is updated
* Architecture rules are respected
* Security rules are respected
* No unnecessary code is introduced
* MCP contracts are clearly defined
* Production readiness is maintained

```
```
