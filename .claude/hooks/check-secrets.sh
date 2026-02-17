#!/bin/bash
# PreToolUse: .kt 파일에 하드코딩된 시크릿 감지 시 차단

FILE_PATH=$(echo "$TOOL_INPUT" | grep -o '"file_path"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*"\([^"]*\)"/\1/')

# .kt 파일이 아니면 통과
if ! echo "$FILE_PATH" | grep -q '\.kt$'; then
  exit 0
fi

# 시크릿 패턴 검사
if echo "$TOOL_INPUT" | grep -qiE '(password|secret|api.?key|token|credential|private.?key|access.?key|secret.?key|conn.?string)[[:space:]]*=[[:space:]]*"[^"$]{3,}'; then
  echo "WARNING: 하드코딩된 시크릿이 감지되었습니다." >&2
  echo "application.properties 또는 환경변수를 사용하세요." >&2
  exit 1
fi
