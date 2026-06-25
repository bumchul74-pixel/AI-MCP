# ADR-002: MCP Tool Design

## Status
Accepted

## Context
MCP Tools expose application capabilities to AI clients.
Poorly designed tools can become unsafe or hard to maintain.

## Decision
All MCP Tools must be explicit, narrow, and testable.

## Rules
- Each tool has one clear responsibility.
- Each tool has input/output DTOs.
- Inputs must be validated.
- Outputs must be structured.
- Prefer read-only tools first.
- Destructive tools require explicit guardrails.

## Consequences
- Tool contracts are easier to understand.
- AI clients can call tools more safely.
- Maintenance cost is reduced.