# rule.md

# Purpose

This file contains implementation rules that must be followed by any AI coding agent working on this repository.

AGENT.md defines architecture and project standards.

This file defines day-to-day coding behavior.

If there is a conflict:

1. AGENT.md
2. rule.md
3. User Request

in that order.

---

# Mandatory Startup Behavior

Before making any code changes:

1. Read AGENT.md
2. Read rule.md
3. Analyze repository structure
4. Identify impacted modules
5. Create a short implementation plan

Never start coding immediately.

---

# Change Scope

Keep changes:

* Small
* Focused
* Reversible

Do not modify unrelated code.

Do not perform large refactors unless explicitly requested.

---

# Architecture Compliance

Always follow Hexagonal Architecture.

Business logic belongs in:

```text
application
domain
```

Never place business logic inside:

```text
bootstrap
config
controller
adapter
```

Domain layer must remain framework-independent.

---

# Java Standards

Target:

```text
Java 21
```

Prefer:

* Record
* Sealed Interface
* Pattern Matching
* Switch Expressions
* Optional
* java.time

Avoid:

* Field Injection
* Legacy Date APIs
* Static State
* Utility God Classes

---

# Spring Standards

Use:

* Constructor Injection
* ConfigurationProperties
* Bean Validation

Avoid:

* @Autowired fields
* Business Logic in Configurations
* Hardcoded Properties

---

# MCP Standards

Every Tool must have:

* Name
* Description
* Input DTO
* Output DTO
* Validation
* Tests

Every Resource must have:

* Purpose
* Access Pattern
* Example Response

Every Prompt must have:

* Clear Intent
* Example Usage

Tool naming must be explicit and self-descriptive.

---

# Testing Rules

For every production code change:

Create or update tests.

Required:

* Unit Test
* Integration Test (when applicable)

Never remove tests without justification.

Never skip tests.

---

# Error Handling

Never expose:

* Stack Trace
* Internal Class Names
* Database Errors

Return structured errors.

Use centralized exception handling.

---

# Security Rules

Never:

* Hardcode credentials
* Hardcode API Keys
* Hardcode Tokens

Never log:

* Passwords
* Secrets
* Tokens

Validate all external inputs.

Sanitize all user-controlled values.

---

# Logging Rules

Use structured logging.

Log:

* Important state changes
* Failures
* External API calls

Do not log sensitive data.

---

# Performance Rules

Avoid:

* N+1 Queries
* Unbounded Collection Loading
* Blocking Calls in Reactive Flows

Use pagination when appropriate.

Measure before optimizing.

---

# Documentation Rules

Update README when:

* New Tool is added
* New Resource is added
* Configuration changes

Keep examples up to date.

---

# Build Verification

Before completing a task:

Run:

```bash
./gradlew clean test
```

If integration verification is required:

```bash
./gradlew clean build
```

Do not claim success without execution.

---

# Response Format

When finishing implementation provide:

## Summary

Brief overview of changes.

## Files Changed

List modified files.

## Tests

Executed tests and results.

## Risks

Potential concerns.

## Next Improvements

Optional future work.

---

# Forbidden Actions

Do not:

* Ignore AGENT.md
* Break architecture boundaries
* Introduce dead code
* Add unnecessary dependencies
* Refactor unrelated modules
* Disable tests
* Bypass validation
* Hardcode environment values

---

# Golden Rule

Prefer maintainability over cleverness.

Prefer readability over brevity.

Prefer architecture consistency over short-term convenience.
