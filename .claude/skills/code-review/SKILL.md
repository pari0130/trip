---
name: code-review
description: 체계적인 코드 리뷰 프로세스 - 기능성, 동시성, 성능, 보안, 테스트, 가독성을 검증합니다. 호텔 예약 시스템에 특화된 체크리스트 포함.
---

# 코드 리뷰 스킬

## 사용 시점
PR 리뷰 | 구현 완료 | 리팩토링 | 버그 수정 검증

## 리뷰 프로세스

### 1. 컨텍스트 이해
- [ ] PR/이슈 설명 명확?
- [ ] 요구사항 파악?
- [ ] 문서 업데이트 (CLAUDE.md, README)?

---

### 2. 기능 정확성
- [ ] 명시된 문제 해결?
- [ ] 엣지 케이스 (null, empty, 경계값)?
- [ ] 비즈니스 규칙 올바름?
- [ ] 데이터 무결성 유지?

```kotlin
// ❌ 검증 없음
fun create(req: Request) = repo.save(req)

// ✅ 포괄적 검증
fun create(req: Request) {
    validateDates(req.checkIn, req.checkOut)
    validateInventory(req.roomTypeId)
    // ...
}
```

---

### 3. 동시성 안전성 ⚠️ 가장 중요!

**필수 체크:**
- [ ] Race condition 방지?
- [ ] Pessimistic/Optimistic lock 올바름?
- [ ] 트랜잭션 경계 적절?
- [ ] 데드락 방지 (ORDER BY)?
- [ ] 재고 작업에 `FOR UPDATE`?
- [ ] `@Version` 존재?
- [ ] 동시성 테스트 통과?

```kotlin
// ❌ Race condition
val inv = repo.findById(id)
if (inv.available > 0) inv.available--

// ✅ Pessimistic lock
@Lock(PESSIMISTIC_WRITE)
@Query("SELECT i FROM Inventory i WHERE i.id = :id ORDER BY i.date")
fun findByIdWithLock(id: Long): Inventory?
```

---

### 4. 성능
- [ ] N+1 쿼리 방지? (`JOIN FETCH`)
- [ ] 적절한 인덱스?
- [ ] 페이지네이션?
- [ ] 읽기 작업에 `readOnly = true`?
- [ ] 리소스 적절히 해제?

```kotlin
// ❌ N+1 문제
hotels.forEach { it.roomTypes }  // N queries

// ✅ JOIN FETCH
@Query("SELECT h FROM Hotel h JOIN FETCH h.roomTypes")
```

---

### 5. 보안
- [ ] 입력 검증 및 정제?
- [ ] SQL Injection 방지? (파라미터화된 쿼리)
- [ ] XSS 취약점 없음?
- [ ] 인가 검사?
- [ ] 민감 데이터 로그 안 됨?
- [ ] 비밀번호 암호화?

---

### 6. 에러 처리
- [ ] 예외가 구체적이고 의미 있음?
- [ ] 조용히 무시하지 않음?
- [ ] 적절한 HTTP 상태 코드?
- [ ] RuntimeException으로 롤백?
- [ ] All-or-nothing (부분 업데이트 없음)?

```kotlin
// ❌ 일반 예외
throw RuntimeException("Error")

// ✅ 구체적 예외
throw InsufficientInventoryException(
    "재고 부족: 룸=$roomTypeId, 날짜=$date"
)
```

---

### 7. 테스트
- [ ] 단위 테스트 (비즈니스 로직)?
- [ ] 통합 테스트 (중요 경로)?
- [ ] 엣지 케이스 커버?
- [ ] 동시성 테스트 (멀티스레드)?
- [ ] 정상/실패 경로 모두?

**동시성 테스트 예시:**
```kotlin
@Test
fun `동시 10개 요청, 재고 3개 → 3개만 성공`() {
    val executor = Executors.newFixedThreadPool(10)
    val latch = CountDownLatch(10)
    val successCount = AtomicInteger(0)

    repeat(10) {
        executor.submit {
            try {
                service.createReservation(req)
                successCount.incrementAndGet()
            } catch (e: InsufficientInventoryException) {
                // 7개는 예외
            } finally { latch.countDown() }
        }
    }
    latch.await()
    assertThat(successCount.get()).isEqualTo(3)
}
```

---

### 8. 코드 품질
- [ ] 설명적이고 일관된 네이밍?
- [ ] 함수가 작고 집중적? (SRP)
- [ ] 적절한 레이어링?
- [ ] 불변성 선호 (`val`)?
- [ ] Null 안전성? (`?.`, `?:`)
- [ ] DTO에 data class?
- [ ] 주석이 WHY 설명? (WHAT 아님)
- [ ] 주석 처리된 코드 없음?

---

### 9. 의존성 및 설정
- [ ] 필요한 의존성만 추가?
- [ ] 버전이 `libs.versions.toml`에?
- [ ] 하드코딩된 비밀 없음?
- [ ] 환경 변수 사용?

### 10. 데이터베이스
- [ ] Flyway 스크립트 멱등성?
- [ ] 적절한 네이밍 (`V1__description.sql`)?
- [ ] JPA 관계 적절?
- [ ] Fetch 전략 최적화 (LAZY)?
- [ ] `@Version` 존재?

---

## 호텔 예약 시스템 특화 체크

### 재고 관리
- [ ] `PESSIMISTIC_WRITE` lock?
- [ ] 날짜 검증 (checkIn < checkOut)?
- [ ] 체크아웃 제외?
- [ ] 복원 시 `totalQuantity` 미초과?

### 예약 생명주기
- [ ] 상태 전이 (CONFIRMED → CANCELLED만)?
- [ ] Soft delete?
- [ ] 재고 원자적 복원?

### 날짜 처리
- [ ] ISO 8601 (`yyyy-MM-dd`)?
- [ ] 타임존 일관 (UTC)?
- [ ] 체크인 포함, 체크아웃 제외?

### API 설계
- [ ] RESTful?
- [ ] 적절한 HTTP 메서드?
- [ ] 의미적 상태 코드 (200, 201, 404, 409)?

---


## 흔한 안티패턴

```kotlin
// ❌ 잠금 없음 → Race condition
fun reserve() {
    val inv = repo.findById(id)
    if (inv.available > 0) inv.available--
}

// ❌ 잘못된 날짜 범위 → 체크아웃 포함
val dates = (checkIn..checkOut).toList()

// ❌ 부분 실패 → 불일치 상태
dates.forEach { deductInventory(it) }

// ✅ 올바른 패턴
@Transactional
fun reserve(req: Request) {
    val dates = (req.checkIn until req.checkOut).toList()
    val invs = repo.findByDatesWithLock(req.roomTypeId, dates)
    invs.forEach { require(it.available >= req.rooms) }
    invs.forEach { it.deduct(req.rooms) }
    reservationRepo.save(...)
}
```

---

## 리뷰 출력 형식

**심각도:**
- 🔴 CRITICAL: 데이터 손상, 보안, 프로덕션 차단
- 🟠 HIGH: 잘못된 동작, 동시성 버그
- 🟡 MEDIUM: 코드 스멜, 테스트 누락
- 🟢 LOW: 스타일, 문서화

**각 이슈:**
```
### [심각도] 이슈 제목
**위치:** `File.kt:123`
**문제:** [설명]
**권장:** [수정 방법]
```

---

## 리뷰 전 자동화 실행

```bash
./gradlew ktlintCheck test build
```

## 리뷰 원칙

- 건설적 개선 제안
- 구체적 예시 제공
- 우선순위: 동시성 > 기능 > 품질
- WHY 설명
- **목표: 안전하고 올바른 코드**