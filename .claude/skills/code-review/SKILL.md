---
name: code-review
description: 체계적인 코드 리뷰 프로세스 - 기능성, 동시성, 성능, 보안, 테스트, 가독성을 검증합니다. 호텔 예약 시스템에 특화된 체크리스트 포함.
---

# 코드 리뷰 스킬

체계적이고 포괄적인 코드 리뷰를 수행합니다. 단순한 스타일 체크를 넘어 기능 정확성, 동시성 안전성, 성능, 보안, 테스트 커버리지를 검증합니다.

## 사용 시점

- PR 리뷰 시
- 구현 완료 후 최종 검증
- 리팩토링 후 품질 확인
- 버그 수정 후 재발 방지 검증
- 새로운 팀원의 코드 온보딩

## 리뷰 프로세스

### 1. 컨텍스트 이해

**코드 리뷰 전 이해해야 할 사항:**
- 이 코드가 해결하는 문제는 무엇인가?
- 요구사항은 무엇인가? (기능 요구사항, 비기능 요구사항)
- 예상 동작은 무엇인가?
- 연관된 이슈나 PR이 있는가?

**체크리스트:**
- [ ] PR/이슈 설명이 명확한가?
- [ ] 관련 문서가 업데이트되었는가?
- [ ] CLAUDE.md 또는 README가 변경사항을 반영하는가?

---

### 2. 기능 정확성

**핵심 기능:**
- [ ] 명시된 문제를 해결하는가?
- [ ] 엣지 케이스 처리 (null, empty, 경계값)?
- [ ] 에러 조건이 적절히 처리되는가?
- [ ] 입력 검증이 포괄적인가?

**비즈니스 로직:**
- [ ] 비즈니스 규칙이 올바르게 구현되었는가?
- [ ] 도메인 제약조건이 강제되는가?
- [ ] 데이터 무결성이 유지되는가?

**예시:**
```kotlin
// ❌ 나쁨: 검증 없음
fun createReservation(request: CreateReservationRequest) {
    reservation.save(request)
}

// ✅ 좋음: 포괄적 검증
fun createReservation(request: CreateReservationRequest) {
    validateDates(request.checkIn, request.checkOut)
    validateInventory(request.roomTypeId, request.dates)
    // ...
}
```

---

### 3. 동시성 안전성

**호텔 예약 시스템에서 가장 중요!**

**체크리스트:**
- [ ] Race condition이 방지되는가?
- [ ] Pessimistic/Optimistic 잠금이 올바르게 사용되는가?
- [ ] 트랜잭션 경계가 적절한가?
- [ ] 데드락 가능성이 제거되었는가?
- [ ] 잠금 순서가 일관적인가?

**위험 신호:**
```kotlin
// ❌ 위험: 잠금 없음 - race condition!
val inventory = inventoryRepository.findById(id)
if (inventory.available > 0) {
    inventory.available -= 1  // 여러 스레드가 동시에 도달 가능!
    inventoryRepository.save(inventory)
}

// ✅ 안전: Pessimistic lock
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT i FROM Inventory i WHERE i.id = :id ORDER BY i.date")
fun findByIdWithLock(id: Long): Inventory?
```

**세부 체크:**
- [ ] 재고 작업에 `SELECT ... FOR UPDATE` 사용?
- [ ] 잠금 순서가 `ORDER BY date ASC`를 따르는가?
- [ ] 트랜잭션 격리 수준이 적절한가?
- [ ] 중요 엔티티에 `@Version` (Optimistic Lock)이 있는가?
- [ ] 동시성 테스트가 작성되고 통과하는가?

---

### 4. 성능

**쿼리 성능:**
- [ ] N+1 쿼리가 방지되는가? (`JOIN FETCH` 또는 DTO projection 사용)
- [ ] 쿼리 컬럼에 적절한 인덱스가 있는가?
- [ ] 대량 결과에 페이지네이션이 적용되는가?
- [ ] 읽기 작업에 `@Transactional(readOnly = true)`를 사용하는가?

**예시:**
```kotlin
// ❌ 나쁨: N+1 문제
val hotels = hotelRepository.findAll()  // 1 query
hotels.forEach { hotel ->
    hotel.roomTypes.forEach { ... }  // N queries!
}

// ✅ 좋음: JOIN FETCH로 단일 쿼리
@Query("SELECT h FROM Hotel h JOIN FETCH h.roomTypes")
fun findAllWithRoomTypes(): List<Hotel>
```

**리소스 관리:**
- [ ] 연결이 적절히 닫히는가?
- [ ] 대용량 객체가 불필요하게 메모리에 유지되지 않는가?
- [ ] 적절한 경우 배치 작업을 사용하는가?

---

### 5. 보안

**입력 검증:**
- [ ] 사용자 입력이 정제되는가?
- [ ] SQL Injection이 방지되는가? (파라미터화된 쿼리 사용)
- [ ] XSS 취약점이 확인되었는가?

**접근 제어:**
- [ ] 인가 검사가 있는가?
- [ ] 사용자가 자신의 데이터만 접근하는가?
- [ ] 관리자 전용 작업이 보호되는가?

**데이터 보호:**
- [ ] 민감한 데이터가 로그되지 않는가?
- [ ] PII(개인식별정보)가 올바르게 처리되는가?
- [ ] 비밀번호가 평문으로 저장되지 않는가?

---

### 6. 에러 처리

**예외 처리:**
- [ ] 예외가 의미 있고 구체적인가?
- [ ] 예외를 조용히 무시하지 않는가?
- [ ] 적절한 HTTP 상태 코드를 반환하는가?
- [ ] 사용자 친화적인 에러 메시지인가?

**예시:**
```kotlin
// ❌ 나쁨: 일반 예외, 컨텍스트 없음
throw RuntimeException("Error")

// ✅ 좋음: 구체적 예외와 컨텍스트
throw InsufficientInventoryException(
    "재고 부족: 룸타입 ID=$roomTypeId, 날짜=$date, 요청=$requested, 잔여=$available"
)
```

**트랜잭션 동작:**
- [ ] RuntimeException이 롤백을 발생시키는가?
- [ ] 부분 업데이트가 불가능한가 (all-or-nothing)?
- [ ] 실패 시 데이터 손상이 없는가?

---

### 7. 테스트

**테스트 커버리지:**
- [ ] 비즈니스 로직의 단위 테스트?
- [ ] 중요 경로의 통합 테스트?
- [ ] 엣지 케이스가 커버되는가?
- [ ] 정상 경로와 실패 경로 모두 테스트되는가?

**동시성 테스트:**
- [ ] Race condition을 위한 멀티스레드 테스트?
- [ ] 데드락 시나리오가 테스트되는가?
- [ ] 부하 상황에서 재고 일관성이 검증되는가?

**예시:**
```kotlin
@Test
fun `동시에 10개 예약 요청 시 재고 3개면 3개만 성공`() {
    val executor = Executors.newFixedThreadPool(10)
    val latch = CountDownLatch(10)
    val successCount = AtomicInteger(0)

    repeat(10) {
        executor.submit {
            try {
                reservationService.createReservation(request)
                successCount.incrementAndGet()
            } catch (e: InsufficientInventoryException) {
                // 7개 스레드는 이 예외 예상
            } finally {
                latch.countDown()
            }
        }
    }
    latch.await()

    assertThat(successCount.get()).isEqualTo(3)
    assertThat(inventoryRepository.findById(id).availableQuantity).isEqualTo(0)
}
```

---

### 8. 코드 품질 및 가독성

**네이밍:**
- [ ] 이름이 설명적이고 의미 있는가?
- [ ] 보편적으로 알려지지 않은 약어를 피하는가?
- [ ] 일관된 네이밍 규칙을 따르는가?

**구조:**
- [ ] 함수가 작고 집중적인가 (Single Responsibility)?
- [ ] 적절한 레이어링 (Controller → Service → Repository)?
- [ ] God 클래스/함수가 없는가?

**Kotlin/Java 모범 사례:**
- [ ] 불변성 선호 (`val` over `var`)?
- [ ] Null 안전성 처리 (`?.`, `?:`, `!!` 올바른 사용)?
- [ ] DTO에 data class 사용?
- [ ] 명확성을 위한 확장 함수 사용?

**주석:**
- [ ] 주석이 WHY를 설명하는가, WHAT이 아니라?
- [ ] 복잡한 로직이 문서화되어 있는가?
- [ ] 주석 처리된 코드가 없는가?

---

### 9. 의존성 및 설정

**의존성:**
- [ ] 필요한 의존성만 추가되었는가?
- [ ] 버전이 `gradle/libs.versions.toml`에 명시되었는가?
- [ ] 의존성에 보안 취약점이 없는가?

**설정:**
- [ ] 하드코딩된 비밀이나 자격 증명이 없는가?
- [ ] 환경별 설정이 `application-{profile}.properties`에 있는가?
- [ ] 민감한 값이 환경 변수를 사용하는가?

---

### 10. 데이터베이스 및 마이그레이션

**Flyway 마이그레이션:**
- [ ] 마이그레이션 스크립트가 멱등성을 가지는가?
- [ ] 하위 호환성이 있는가?
- [ ] 적절한 네이밍 (`V{version}__{description}.sql`)?
- [ ] 백업 계획 없이 파괴적 변경이 없는가?

**JPA 엔티티:**
- [ ] 적절한 관계 (`@OneToMany`, `@ManyToOne`)?
- [ ] Cascade 타입이 적절한가?
- [ ] Fetch 전략이 최적화되었는가 (LAZY vs EAGER)?
- [ ] Optimistic locking을 위한 `@Version`이 있는가?

---

## 호텔 예약 시스템 특화 체크

### 재고 관리
- [ ] 재고 작업에 `PESSIMISTIC_WRITE` lock 사용?
- [ ] 날짜 범위 검증 (checkIn < checkOut)?
- [ ] 체크아웃 날짜가 예약 박수에서 제외되는가?
- [ ] 복원 시 `totalQuantity`를 초과하지 않는가?

### 예약 생명주기
- [ ] 상태 전이가 유효한가 (CONFIRMED → CANCELLED만)?
- [ ] Soft delete 구현 (하드 삭제 없음)?
- [ ] 취소 시 재고를 원자적으로 복원하는가?

### 날짜 처리
- [ ] 날짜가 ISO 8601 형식 (`yyyy-MM-dd`)?
- [ ] 타임존이 일관되게 처리되는가 (UTC 권장)?
- [ ] 날짜 범위가 체크인 포함, 체크아웃 제외?

### API 설계
- [ ] RESTful 규칙을 따르는가?
- [ ] 적절한 HTTP 메서드 (GET, POST, PATCH, DELETE)?
- [ ] 상태 코드가 의미적인가 (200, 201, 400, 404, 409, 503)?
- [ ] 목록 엔드포인트에 페이지네이션?

---

## 리뷰 체크리스트 템플릿

각 PR 리뷰 시 복사해서 사용:

```markdown
## 기능 리뷰
- [ ] 요구사항 충족
- [ ] 엣지 케이스 처리
- [ ] 비즈니스 로직 정확

## 동시성 및 안전성
- [ ] Race condition 방지
- [ ] 적절한 잠금 전략
- [ ] 데드락 없음
- [ ] 트랜잭션 경계 적절

## 성능
- [ ] N+1 쿼리 없음
- [ ] 적절한 인덱스
- [ ] 읽기 전용 트랜잭션 사용

## 보안
- [ ] 입력 검증
- [ ] 인가 확인
- [ ] 민감 데이터 노출 없음

## 테스트
- [ ] 단위 테스트 존재
- [ ] 동시성 테스트 포함
- [ ] 엣지 케이스 커버

## 코드 품질
- [ ] 깔끔하고 가독성 좋음
- [ ] 적절한 네이밍
- [ ] 코드 스멜 없음

## 문서화
- [ ] README 업데이트
- [ ] API 문서 최신
- [ ] 의미 있는 주석
```

---

## 호텔 예약 시스템의 흔한 안티패턴

### ❌ 안티패턴 1: 잠금 없음
```kotlin
// 위험: 여러 사용자가 같은 방을 예약할 수 있음
fun reserve() {
    val inventory = repo.findById(id)
    if (inventory.available > 0) {
        inventory.available--
        repo.save(inventory)
    }
}
```

### ❌ 안티패턴 2: 잘못된 날짜 범위
```kotlin
// 잘못됨: 체크아웃 날짜 포함 (2박인데 3박으로 계산)
val dates = (checkIn..checkOut).toList()
```

### ❌ 안티패턴 3: 부분 실패
```kotlin
// 나쁨: 1일차 성공, 2일차 실패 → 불일치 상태
dates.forEach { date ->
    deductInventory(date)  // 이후 날짜 실패 시 롤백 없음!
}
```

### ✅ 올바른 패턴
```kotlin
@Transactional
fun reserve(request: CreateReservationRequest) {
    val dates = (request.checkIn until request.checkOut).toList()

    // 모든 날짜를 한 번에 잠금
    val inventories = inventoryRepo.findByRoomTypeAndDatesWithLock(
        request.roomTypeId, dates
    )

    // 수정 전에 모두 검증
    inventories.forEach {
        require(it.availableQuantity >= request.numberOfRooms) {
            throw InsufficientInventoryException(...)
        }
    }

    // 모두 차감 (all-or-nothing)
    inventories.forEach { it.deduct(request.numberOfRooms) }

    // 예약 저장
    reservationRepo.save(...)
}
```

---

## 리뷰 출력 형식

**발견된 각 이슈에 대해:**

```markdown
### [심각도] 이슈 제목

**위치:** `FileName.kt:123`

**문제:**
[무엇이 잘못되었는지 설명]

**영향:**
[중요한 이유 - 보안 위험, 데이터 손상, 성능 등]

**권장사항:**
[구체적 수정 방법]

**예시:**
```kotlin
// ❌ 현재 (나쁨)
...

// ✅ 제안 (좋음)
...
```
```

**심각도 수준:**
- 🔴 **심각 (CRITICAL)**: 데이터 손상, 보안 취약점, 프로덕션 차단
- 🟠 **높음 (HIGH)**: 잘못된 동작, 성능 이슈, 동시성 버그
- 🟡 **중간 (MEDIUM)**: 코드 스멜, 유지보수성 이슈, 테스트 누락
- 🟢 **낮음 (LOW)**: 스타일, 네이밍, 문서화

---

## 자동화 도구 통합

수동 리뷰 전에 실행:

```bash
# Lint
./gradlew ktlintCheck

# 테스트
./gradlew test

# 빌드
./gradlew build
```

이 도구들의 출력을 먼저 검토한 후 수동 리뷰를 진행합니다.

---

## 기억할 점

- **건설적으로**: 비판만 하지 말고 개선 제안
- **구체적으로**: 정확한 라인을 가리키고 예시 제공
- **우선순위 설정**: 중요한 이슈에 먼저 집중
- **교육하기**: 무엇이 문제인지 WHY를 설명
- **검증하기**: 권장사항을 테스트한 후 제안

**목표는 완벽한 코드가 아니라, 안전하고 유지보수 가능하며 올바른 코드입니다.**