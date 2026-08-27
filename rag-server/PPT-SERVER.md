# PPT Generation MCP Server

This Python MCP server analyzes uploaded PowerPoint templates and generates editable
`.pptx` decks from user content. It uses [AutoPPT](https://github.com/yeasy/AutoPPT)
0.6.0 (Apache-2.0) as the generation engine and exposes Streamable HTTP at `/ppt`.

## Why AutoPPT

- Native, editable PPTX output
- Existing corporate PPTX template support
- Content-aware slide planning and richer layouts
- Built-in deck QA and deterministic offline mock mode
- Smaller Windows integration surface than Docker-first Presenton or the
  Linux/WSL-only PPTAgent toolchain

## Start

From `AI-MCP/rag-server`:

```powershell
./start-ppt-server.ps1
```

The default endpoint is `http://127.0.0.1:8002/ppt`. The first run creates the
isolated `.venv-ppt`, installs `requirements-ppt-mcp.txt`, and creates input/output
directories.

Useful options:

```powershell
./start-ppt-server.ps1 -Restart
./start-ppt-server.ps1 -SkipInstall
./start-ppt-server.ps1 -Provider openai -Model gpt-5
./start-ppt-server.ps1 -Offline
./start-ppt-server.ps1 -Port 8012 -OutputDirectory "D:/ppt-output"
./start-ppt-server.ps1 -AllowedDirectories "D:/templates;D:/uploads"
```

Provider credentials are read from environment variables such as
`OPENAI_API_KEY`, `GOOGLE_API_KEY`, or `ANTHROPIC_API_KEY`. For an OpenAI-compatible
local endpoint, set `OPENAI_API_BASE`. Never put credentials in this repository.

## MCP tools

### `analyze_ppt_template`

Input:

```json
{
  "file_path": "inbox/company-template.pptx"
}
```

Returns slide size, master/layout inventory, placeholder roles and capabilities,
plus a source-slide text/shape inventory.

### `generate_presentation`

Input:

```json
{
  "content": "2026년 하반기 기술 전환 계획과 핵심 투자 과제",
  "template_path": "inbox/company-template.pptx",
  "output_name": "2026-tech-roadmap.pptx",
  "slides": 10,
  "language": "Korean",
  "provider": "openai",
  "model": "gpt-5",
  "style": "corporate",
  "offline": false,
  "thumbnails": false
}
```

The tool validates the template, confines file access to `PPT_MCP_ALLOWED_DIRS`,
runs AutoPPT in an isolated child process, verifies the generated PPTX package,
and returns its path, size, SHA-256 digest, and engine metadata.

## Test

```powershell
./.venv-ppt/Scripts/python.exe -m unittest discover -s tests -p "test_ppt_mcp.py"
```
