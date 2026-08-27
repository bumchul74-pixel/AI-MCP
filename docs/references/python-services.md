# 외부 Python 서비스 실행

Python RAG, OCR과 PPT 생성 구현은 AI-MCP 저장소가 소유한다. AI-AGENT는 아래 네트워크 계약에만 의존한다.

| 서비스 | AI-MCP 소스 위치 | 기본 endpoint |
| --- | --- | --- |
| RAG REST | `rag-server` | `http://localhost:8000` |
| EasyOCR MCP | `rag-server/app/ocr` | `http://localhost:8001/ocr` |
| PPT MCP | `rag-server/app/ppt` (runtime: `.venv-ppt`) | `http://localhost:8002/ppt` |

로컬에서는 AI-MCP 저장소에서 각 시작 스크립트를 실행한다. AI-AGENT의 연결 주소는 환경변수 `RAG_SERVER_BASE_URL`, `EASYOCR_MCP_BASE_URL`, `PPT_MCP_URL`로 구성하고 실제 credential을 저장소에 기록하지 않는다.

Python 단위 테스트와 의존성 설치도 AI-MCP에서 수행한다. AI-AGENT의 `verifyAll`은 외부 Python 구현을 직접 실행하지 않는다.
