# Python 서비스 AI-MCP 이전

## 목표

AI-AGENT의 `rag-server` 소스와 실행 책임을 AI-MCP로 이전하고, 독립 PPT MCP 모듈을 `AI-MCP/rag-server/app/ppt`에 배치한다.

## 제약

- RAG, OCR, PPT는 같은 `rag-server` 아래에 있어도 각자 독립 프로세스와 의존성 경계를 유지한다.
- 기존 사용자 변경, 로컬 데이터, 가상환경과 로그를 손실하지 않는다.
- AI-AGENT는 Python 구현에 직접 의존하지 않고 REST/MCP 계약에만 의존한다.
- 실제 외부 LLM을 사용하는 PPT 생성은 검증하지 않는다.

## 비목표

- RAG, OCR 또는 PPT 생성 알고리즘 변경
- REST/MCP endpoint와 포트 변경
- 업로드 데이터나 vector store migration

## 완료 상태

- RAG와 EasyOCR 소스는 `AI-MCP/rag-server`로 이전되었다.
- PPT MCP는 `AI-MCP/rag-server/app/ppt`에 배치되었으며 `.venv-ppt` 독립 런타임을 유지한다.
- AI-AGENT는 네트워크 계약에만 의존하고 Python 구현 검증은 AI-MCP가 소유한다.

## 단계

1. Python 서비스 소스를 `AI-MCP/rag-server`로 이동하고 PPT 모듈을 `app/ppt`에 통합한다.
2. AI-AGENT에서 Python 테스트 구현 소유권과 로컬 경로 참조를 제거한다.
3. AI-MCP 문서와 실행 경로를 새 구조로 갱신한다.
4. RAG, OCR, PPT Python 테스트와 Java/Frontend 결정적 gate를 실행한다.

## 검증

- AI-MCP `rag-server` Python unittest
- AI-MCP `rag-server/.venv-ppt` PPT unittest
- AI-MCP `gradlew.bat clean test`
- AI-AGENT `gradlew.bat verifyAll`

## 위험

- 저장소 간 이동은 Git에서 rename이 아니라 AI-AGENT 삭제와 AI-MCP 추가로 표시된다.
- 로컬 `.venv`와 `.venv-ppt`의 절대 경로 metadata가 이동 후 유효하지 않을 수 있어 실행 시 재생성이 필요할 수 있다.
- 중첩 구조가 런타임 결합으로 오해되지 않도록 문서와 requirements를 분리한다.

## Rollback

이동한 `AI-MCP/rag-server`를 `AI-AGENT/rag-server`로 되돌리고, `app/ppt` 모듈과 `.venv-ppt` 경로 변경을 역적용한다.

## 결정

단일 `rag-server` 구조를 사용하되 RAG의 `.venv`와 PPT의 `.venv-ppt`, requirements, 시작 스크립트, 포트와 프로세스를 공유하지 않는다.


## 완료 증거

- `gradlew.bat pythonServicesTest`: 성공 (RAG/OCR 16개, PPT 4개)
- AI-MCP `gradlew.bat clean test`: 성공
- AI-AGENT `gradlew.bat verifyAll`: 성공
- 원본 `AI-AGENT/rag-server`: 제거 확인
- 대상 RAG/PPT 디렉터리와 두 가상환경: 존재 및 실행 확인
