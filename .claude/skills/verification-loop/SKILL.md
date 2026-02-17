---
name: verification-loop
description: 빌드-테스트-검증 루프 — PR 전, 주요 변경 후 실행하는 종합 검증 패턴
---

# Verification Loop

## 검증 단계

### 1. 빌드 검증

```bash
./gradlew build
```

실패 시 즉시 중단. 빌드 에러 먼저 해결.

### 2. 테스트 실행

```bash
./gradlew test
```

전체 테스트 통과 확인. 실패 테스트 0개 목표.

### 3. 동시성 검증

```bash
./gradlew test --tests "*ConcurrencyTest"
```

재고 정합성 핵심 검증:
- 초과 판매 없음
- 재고 음수 없음
- 성공 수 = 초기 재고 (재고 < 스레드 수일 때)

### 4. API 스모크 테스트

```bash
# bootRun 후 (샘플 데이터: 오늘~29일 후 재고)
TODAY=$(date +%Y-%m-%d)
CHECKOUT=$(date -v+2d +%Y-%m-%d)

# 재고 조회
curl -s "http://localhost:8080/api/v1/inventory?roomTypeId=1&checkInDate=${TODAY}&checkOutDate=${CHECKOUT}"

# 예약 생성
curl -s -X POST http://localhost:8080/api/v1/reservations \
  -H "Content-Type: application/json" \
  -d "{\"roomTypeId\":1,\"guestName\":\"테스트\",\"guestEmail\":\"test@test.com\",\"checkInDate\":\"${TODAY}\",\"checkOutDate\":\"${CHECKOUT}\",\"numberOfRooms\":1}"

# 예약 조회
curl -s http://localhost:8080/api/v1/reservations/1

# 예약 취소
curl -s -X PATCH http://localhost:8080/api/v1/reservations/1/cancel
```

### 5. 검증 보고서

```
VERIFICATION: [PASS/FAIL]

Build:        [OK/FAIL]
Tests:        [X/Y passed]
Concurrency:  [OK/FAIL - N threads, M success]
API Smoke:    [OK/FAIL]

Ready for PR: [YES/NO]
```

## 실행 시점

- 새 기능 구현 완료 후
- 버그 수정 후
- 리팩토링 후
- PR 생성 전
- 머지 전
