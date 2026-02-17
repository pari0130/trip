#!/bin/bash
# PostToolUse: 소스 코드 변경 시 CLAUDE.md / README.md 업데이트 알림
#
# 구조적 변경(파일 생성, 컨트롤러/서비스/엔티티 수정)이 감지되면
# Claude에게 문서 업데이트를 지시합니다.

FILE_PATH=$(echo "$TOOL_INPUT" | grep -o '"file_path"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*"\([^"]*\)"/\1/')

# 파일 경로가 없으면 종료
if [ -z "$FILE_PATH" ]; then
  exit 0
fi

# CLAUDE.md, README.md 자체 수정은 무시 (무한 루프 방지)
if echo "$FILE_PATH" | grep -qiE '(CLAUDE|README)\.md$'; then
  exit 0
fi

# hook 스크립트, 설정 파일 수정도 무시
if echo "$FILE_PATH" | grep -qE '\.(sh|json)$'; then
  exit 0
fi

# 구조적 변경 감지: Controller, Service, Entity, Repository, DTO, Exception, SQL, build.gradle
if echo "$FILE_PATH" | grep -qiE '(controller|service|repository|entity|dto|exception|migration|build\.gradle)'; then
  echo ""
  echo "[Hook] 구조적 코드 변경 감지: $FILE_PATH"
  echo "[Hook] CLAUDE.md와 README.md에 해당 변경사항을 반영하세요."
  echo "[Hook] - CLAUDE.md: 코딩 컨벤션, 도메인 규칙, 프로젝트 구조 업데이트"
  echo "[Hook] - README.md: API 설명, 설계 의도, 프로젝트 구조 업데이트"
fi
