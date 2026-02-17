# /code-review - 코드 리뷰

변경된 코드를 검토합니다. $ARGUMENTS 가 있으면 해당 파일/범위만 리뷰합니다.

> 상세 리뷰 기준, 안티패턴, 예시는 `.claude/skills/code-review` 스킬을 참조한다.

## 워크플로우

1. `git diff --staged` 및 `git diff`로 변경사항 수집
2. 변경된 파일의 전체 컨텍스트 파악
3. `.claude/skills/code-review` 스킬의 리뷰 프로세스 및 체크리스트를 적용하여 검토
4. 결과 보고

## 보고 형식

```
[SEVERITY] 이슈 제목
File: path/to/file.kt:line
Issue: 구체적 문제
Fix: 제안 수정 방법
```

## 요약

```
| Severity | Count | Status |
|----------|-------|--------|
| CRITICAL | 0     | pass   |
| HIGH     | 0     | pass   |
| MEDIUM   | 0     | info   |

Verdict: APPROVE / WARNING / BLOCK
```
