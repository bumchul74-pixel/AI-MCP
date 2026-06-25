# ADR-003: Security Baseline

## Status
Accepted

## Context
MCP Servers may expose internal systems to AI clients.
This requires strong safety boundaries.

## Decision
Apply secure-by-default implementation rules.

## Rules
- No hardcoded secrets.
- No arbitrary file access.
- No arbitrary shell execution.
- Validate all external inputs.
- Sanitize user-controlled values.
- Never log secrets or tokens.
- Use least privilege for integrations.

## Consequences
- Some tool implementations may require extra validation.
- Risk of unintended data exposure is reduced.