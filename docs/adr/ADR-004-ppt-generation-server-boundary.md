# ADR-004: PPT generation server boundary

## Status
Accepted

## Context

PPT template analysis and generation use a Python runtime, AutoPPT, python-pptx, and provider-specific credentials. The initial implementation lived under a directory named `rag-server`, even though it does not perform retrieval, embedding, indexing, or OCR. That placement makes runtime ownership and dependency installation ambiguous.

## Decision

PPT generation is an independent downstream MCP server implemented in `rag-server/app/ppt` and executed with `rag-server/.venv-ppt`.

- `rag-server/app/ppt` exclusively owns PPTX validation, template analysis, AutoPPT execution, output verification, its `.venv-ppt` Python virtual environment, and its input/output directories. Its physical nesting does not make it part of the RAG process.
- The Spring AI-MCP server depends only on the Streamable HTTP contract at `/ppt`.
- OCR and RAG services must not import PPT generation modules or install PPT generation dependencies.
- PPT file access remains restricted by `PPT_MCP_ALLOWED_DIRS`, and generated files remain confined to `PPT_MCP_OUTPUT_DIR`.

## Consequences

- PPT, OCR, and RAG processes can be installed, started, tested, and scaled independently.
- AutoPPT and provider dependency changes cannot affect OCR or RAG environments.
- Operators must start the PPT MCP server separately before enabling its downstream connection.
