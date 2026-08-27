# ADR-005: Python 서비스 구현과 검증의 AI-MCP 소유권

- 상태: Accepted
- 날짜: 2026-08-21

## 상황

Python RAG 구현이 AI-AGENT 저장소에 있고 PPT MCP가 AI-MCP 저장소에 분리되어 있어, Python 런타임의 소유권과 검증 책임이 두 저장소에 걸쳐 있었다.

## 결정

RAG, OCR과 PPT 생성 Python 구현은 AI-MCP 저장소가 소유한다. 물리적 구조는 `AI-MCP/rag-server` 아래의 `app/ppt` 모듈을 사용하며, PPT 서버는 별도 `.venv-ppt`, requirements, 시작 스크립트, 포트와 프로세스를 유지한다.

AI-AGENT는 RAG REST 및 MCP 계약에만 의존하며 Python 구현 테스트를 `verifyAll`에 포함하지 않는다. 각 Python 단위 테스트는 AI-MCP의 검증 task에서 실행한다.

## 결과

- AI-AGENT는 배포 시 AI-MCP의 로컬 디렉터리 구조에 의존하지 않는다.
- Python 서비스 변경과 의존성 설치는 AI-MCP에서 독립적으로 검증한다.
- 저장소 간 파일 이동은 AI-AGENT 삭제와 AI-MCP 추가로 Git에 기록된다.
