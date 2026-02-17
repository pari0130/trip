# /code-review - 코드 리뷰

변경된 코드를 검토합니다. $ARGUMENTS 가 있으면 해당 파일/범위만 리뷰합니다.

## 워크플로우

1. `git diff --staged` 및 `git diff`로 변경사항 수집
2. 변경된 파일의 전체 컨텍스트 파악
3. 아래 체크리스트 순서대로 검토
4. 결과 보고

## 체크리스트 (우선순위 순)

### CRITICAL
- [ ] 재고 변경 시 `findByRoomTypeIdAndDateRangeForUpdate` 사용 여부
- [ ] `@Transactional` 경계 정확성
- [ ] SQL 인젝션: JPQL 파라미터 바인딩 사용 여부
- [ ] 하드코딩된 시크릿 노출

### HIGH
- [ ] 재고 음수 가능성 (차감 전 검증)
- [ ] totalQuantity 초과 가능성 (복원 시 상한 체크)
- [ ] N+1 쿼리 (JOIN FETCH 필요 여부)
- [ ] 에러 핸들링 누락
- [ ] Request DTO에 Validation 적용 여부

### MEDIUM
- [ ] readOnly 트랜잭션 설정
- [ ] 매직 넘버 사용
- [ ] 네이밍 컨벤션 준수
- [ ] 테스트 누락

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
