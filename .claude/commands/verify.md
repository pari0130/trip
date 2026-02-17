# /verify - 빌드 + 테스트 + API 검증

전체 검증 루프를 실행합니다.

## 워크플로우

### Step 1: 빌드 확인
```bash
./gradlew clean build 2>&1
```
- 실패 시: 에러 보고 후 중단

### Step 2: 테스트 확인
```bash
./gradlew test 2>&1
```
- 실패 시: 실패한 테스트 목록과 원인 보고

### Step 3: 애플리케이션 기동 확인
```bash
./gradlew bootRun &
# 기동 완료 대기 (health check 또는 로그 확인)
sleep 15

# 오늘 기준 상대 날짜로 테스트 (샘플 데이터: 오늘~29일 후)
TODAY=$(date +%Y-%m-%d)
CHECKOUT=$(date -v+2d +%Y-%m-%d)
curl -s "http://localhost:8080/api/v1/inventory?roomTypeId=1&checkInDate=${TODAY}&checkOutDate=${CHECKOUT}"
kill %1
```
- 응답 정상 여부 확인

### Step 4: 결과 요약

```
## 검증 결과

| 항목 | 상태 | 비고 |
|------|------|------|
| 빌드 | PASS/FAIL | - |
| 테스트 (N개) | PASS/FAIL | 실패: X개 |
| API 기동 | PASS/FAIL | - |

종합: READY / NOT READY
```

## $ARGUMENTS 옵션
- `build` - 빌드만 확인
- `test` - 테스트만 확인
- `api` - API 기동만 확인
- (빈 값) - 전체 검증
