from __future__ import annotations

import json
import os
import sys
from pathlib import Path
from typing import Any


def _load_request(request_path: Path) -> dict[str, Any]:
    with request_path.open("r", encoding="utf-8") as request_file:
        payload = json.load(request_file)
    if not isinstance(payload, dict):
        raise ValueError("Generation request must be a JSON object.")
    return payload


def run(request_path: Path) -> Path:
    payload = _load_request(request_path)
    if payload.get("offline"):
        os.environ["AUTOPPT_OFFLINE"] = "1"

    from autoppt.config import Config
    from autoppt.generator import Generator

    provider = str(payload["provider"])
    Config.initialize(configure_logging=True)
    if provider != "mock":
        Config.validate(provider)

    output_path = Path(payload["output_path"]).resolve()
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with Generator(provider_name=provider, model=payload.get("model")) as generator:
        result = generator.generate(
            str(payload["content"]),
            style=str(payload["style"]),
            output_file=str(output_path),
            slides_count=int(payload["slides"]),
            language=str(payload["language"]),
            template_path=str(payload["template_path"]),
            create_thumbnails=bool(payload.get("thumbnails", False)),
        )
    return Path(result).resolve()


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: python -m app.ppt.autoppt_runner <request.json>")
    result = run(Path(sys.argv[1]).resolve())
    print(json.dumps({"output_path": str(result)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
