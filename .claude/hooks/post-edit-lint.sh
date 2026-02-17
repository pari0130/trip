#!/bin/bash
# PostToolUse: .kt 파일 수정 시 ktlintFormat 자동 실행, 기타 파일은 빌드 권장 알림

PROJECT_DIR="$(git rev-parse --show-toplevel 2>/dev/null)"

if [ -z "$PROJECT_DIR" ]; then
  exit 0
fi

FILE_PATH=$(echo "$TOOL_INPUT" | grep -o '"file_path"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*"\([^"]*\)"/\1/')

# 파일 경로가 없으면 종료
if [ -z "$FILE_PATH" ]; then
  exit 0
fi

# .kt 파일 → ktlint 자동 포맷
if echo "$FILE_PATH" | grep -qE '\.kt$'; then
  cd "$PROJECT_DIR"
  JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null) ./gradlew ktlintFormat -q 2>&1 | tail -5
  echo "[Hook] ktlint format 완료: $FILE_PATH"
  exit 0
fi

# .properties, .sql, .kts 파일 → 빌드 권장 알림
if echo "$FILE_PATH" | grep -qE '\.(properties|sql|kts)$'; then
  echo "[Hook] 파일 수정됨: $FILE_PATH - 빌드 검증을 권장합니다 (./gradlew build)"
fi
