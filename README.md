# Trip - 호텔 예약 API 서버

호텔 객실 재고를 관리하고, 여러 사용자가 동시에 예약하더라도 재고 정합성을 보장하는 REST API 서버입니다.

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Kotlin 1.9.25 |
| Framework | Spring Boot 3.2.5 |
| ORM | Spring Data JPA (Hibernate) |
| Database | H2 (in-memory) |
| Migration | Flyway |
| Docs | SpringDoc OpenAPI (Swagger UI) |
| Lint | ktlint |
| Build | Gradle (Kotlin DSL) + Version Catalog |

## 빌드 및 실행

### 요구사항

- **Java 21** (컴파일/실행 대상)

> **Java 25 환경인 경우**: Kotlin 1.9.x가 Java 25를 인식하지 못해 빌드가 실패합니다.
> `~/.gradle/gradle.properties`에 아래 설정을 추가하세요.
> ```properties
> org.gradle.java.home=/path/to/java-21
> ```
> macOS 예시: `org.gradle.java.home=/Library/Java/JavaVirtualMachines/corretto-21/Contents/Home`

```bash
# 빌드 (lint + 컴파일 + 테스트)
./gradlew build

# 실행
./gradlew bootRun

# 테스트만
./gradlew test

# lint 검사 / 자동 수정
./gradlew ktlintCheck
./gradlew ktlintFormat
```

실행 후 접근 가능:
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:tripdb`, username: `sa`)

## API 설명

### 1. 재고 조회

재고는 **날짜별 x 룸타입별 매트릭스**로 관리합니다. 각 셀이 하나의 `Inventory` 레코드입니다.

```
                  | 7/01 | 7/02 | 7/03 | ...
------------------|------|------|------|----
그랜드 디럭스      |  10  |  10  |  10  |
프리미어 스위트     |   5  |   5  |   5  |
로얄 스위트        |   3  |   3  |   3  |
```

- `GET /api/v1/inventory?roomTypeId=&checkInDate=&checkOutDate=`
- 날짜 범위: **체크인일(포함) ~ 체크아웃일(미포함)** (7/1~7/3 → 7/1, 7/2 2박)
- 잠금 없이 조회 (`@Transactional(readOnly = true)`)

### 2. 예약 생성

- `POST /api/v1/reservations`
- 해당 날짜 범위의 재고를 `SELECT ... FOR UPDATE`로 잠금 후 차감
- 모든 날짜에 `availableQuantity >= numberOfRooms` 검증 — 하나라도 부족하면 **전체 실패** (부분 차감 없음)
- 재고 차감 + 예약 저장을 하나의 트랜잭션으로 원자적 처리
- 응답에 `reservationId`가 포함되며, 이 값으로 조회/취소를 수행

### 3. 예약 조회

- `GET /api/v1/reservations/{reservationId}`
- 잠금 없이 단순 조회

### 4. 예약 취소

- `PATCH /api/v1/reservations/{reservationId}/cancel`
- `CONFIRMED → CANCELLED` 단방향 전이 (재활성화 불가)
- 재고 복원 시 `totalQuantity` 초과 방지
- soft delete 방식: `status`만 변경, 레코드는 유지

### 5. 에러 처리

| HTTP 상태 | 발생 조건 | 처리 예외 |
|-----------|----------|----------|
| 400 | 입력 유효성 검사 실패 (필수값 누락, 형식 오류) | `MethodArgumentNotValidException` |
| 400 | 필수 쿼리 파라미터 누락 | `MissingServletRequestParameterException` |
| 400 | 날짜 범위 오류 (체크인 ≥ 체크아웃) | `IllegalArgumentException` |
| 400 | 파라미터 타입 변환 실패 (잘못된 날짜 형식 등) | `MethodArgumentTypeMismatchException` |
| 404 | 존재하지 않는 룸 타입 또는 예약 | `RoomTypeNotFoundException`, `ReservationNotFoundException` |
| 409 | 재고 부족 | `InsufficientInventoryException` |
| 409 | 이미 취소된 예약 | `InvalidReservationStateException` |
| 500 | 예상치 못한 서버 오류 | `Exception` (fallback) |
| 503 | 잠금 타임아웃 (동시 접근 과부하) | `PessimisticLockException` |

> 상세 API 스펙은 Swagger UI 참조: http://localhost:8080/swagger-ui.html

## 설계 시 주요 고려 사항

### 1. 동시 예약 시 재고 정합성

**2중 동시성 제어 전략:**

| 제어 방식                                  | 역할 | 구현 |
|----------------------------------------|------|------|
| **In-Memory 카운터** (1차 필터, redis 처럼 생각) | DB 접근 전 사전 차단으로 불필요한 잠금 경합 방지 | `ConcurrentHashMap` + `AtomicInteger` |
| **Pessimistic Lock** (최종 보장)           | DB 수준에서 순차 처리로 정합성 보장 | `SELECT ... FOR UPDATE` + `ORDER BY date ASC` |

- `FOR UPDATE`로 재고 행을 잠그고, `ORDER BY date ASC`로 잠금 순서를 통일하여 데드락을 방지합니다.
- 잠금 타임아웃 5초 (`jakarta.persistence.lock.timeout=5000`), 초과 시 503 응답.
- In-Memory 카운터는 성능 최적화 레이어이며, DB가 유일한 정합성 입니다.

### 2. 대량 트래픽 대비: Cache 카운터 Pre-Filter

선착순 예매처럼 트래픽이 집중될 때, 재고 3개에 1,000건이 몰리면 997건은 실패 확정임에도 전부 DB 커넥션 + `FOR UPDATE` 대기열에 진입합니다. Redis `DECR` 패턴의 핵심 아이디어 — **원자적 감소 연산으로 DB 진입 전에 걸러내기** — 를 로컬 `AtomicInteger`로 구현했습니다.

```
요청 → [카운터 tryDecrement] → 실패 → 즉시 거절 (DB 접근 없음)
                              → 성공 → [DB FOR UPDATE + 검증 + 차감]
                                        → 실패 → 카운터 보상(increment)
                                        → 성공 → 완료
```

**Race Condition 핵심**: "조회 → 판단 → 차감"을 원자 연산으로 묶어야 합니다.

Redis에서는 `DECR`(단일 키) / Lua 스크립트(다중 키), 본 구현에서는 `AtomicInteger.addAndGet()`(CAS)으로 동일 보장.

**설계 제약**:
- 카운터는 힌트이며, DB가 유일한 정합성 — 카운터 통과 후에도 반드시 `FOR UPDATE`로 최종 검증
- 다중 날짜 중간 실패 시 이미 차감한 날짜를 명시적으로 복원 (DB는 자동 롤백되지만 카운터는 트랜잭션 밖)
- 로컬 `AtomicInteger`는 단일 인스턴스 전용 — 다중 인스턴스 시 Redis `DECR`로 교체 필요 (`tryDecrement`/`increment` 인터페이스 유지로 교체 범위 한정)

### 3. 트랜잭션과 롤백

모든 비즈니스 예외가 `RuntimeException` → `@Transactional` 자동 롤백. 3박 예약 중 2일차 실패 시 1일차 차감도 함께 롤백(All or Nothing). JPA dirty checking은 커밋 시점에 flush하므로 예외 시 DB 반영 자체가 일어나지 않습니다.

### 4. 예약 생명주기

`CONFIRMED → CANCELLED` 단방향 전이. soft delete 방식으로 `status`만 변경하고 재고 복원. 이미 취소된 예약 재취소 시 409.

## 프로젝트 구조

```
src/main/kotlin/com/trip/hotel/
├── TripApplication.kt
├── domain/
│   ├── entity/          Hotel, RoomType, Inventory, Reservation, ReservationStatus, Guest
│   └── repository/      HotelRepository, RoomTypeRepository, InventoryRepository, ReservationRepository, GuestRepository
├── service/             InventoryService, ReservationService, InventoryCounterService
├── config/              OpenApiConfig, InventoryCounterInitializer
├── controller/          InventoryController, ReservationController
├── dto/
│   ├── request/         CreateReservationRequest
│   └── response/        InventoryResponse, ReservationResponse, ErrorResponse
└── exception/           BusinessException, GlobalExceptionHandler + 구체 예외 클래스

src/main/resources/
├── application.properties
└── db/migration/
    ├── V1__create_schema.sql                DDL (hotel, room_type, inventory, reservation)
    ├── V2__insert_sample_data.sql           시드 데이터 (호텔 2, 룸타입 5, 30일 재고)
    └── V3__create_guest_table.sql           guest 테이블 생성 + reservation FK 마이그레이션
```

| 레이어 | 역할 |
|--------|------|
| **Controller** | HTTP 요청/응답 처리, 입력 유효성 검사 |
| **Service** | 비즈니스 로직, 트랜잭션 관리, 동시성 제어 |
| **Repository** | 데이터 접근, 잠금 쿼리 |
| **Entity** | 도메인 모델, 재고 차감/복원 로직 |
| **Exception** | 비즈니스 예외 → HTTP 상태 코드 매핑 |

## 테스트 전략

| 유형 | 대상 | 방식 | 핵심 검증 |
|------|------|------|----------|
| Unit | Service 로직 | Mockito | 예약 생성/취소/조회, 예외 처리 |
| Integration | 동시성 | @SpringBootTest | 10개 스레드 동시 예약 → 재고 정합성 |
| Controller | API | MockMvc | 요청/응답, 상태 코드, 유효성 검사 |
| Repository | 쿼리 | @DataJpaTest | 잠금 쿼리, 날짜 범위 조회 |

### 동시성 테스트

```kotlin
val executorService = Executors.newFixedThreadPool(10)
val latch = CountDownLatch(10)
val successCount = AtomicInteger(0)

repeat(10) {
    executorService.submit {
        try {
            reservationService.createReservation(request)
            successCount.incrementAndGet()
        } catch (e: Exception) { }
        finally { latch.countDown() }
    }
}
latch.await()

// 재고 3개 → 정확히 3개만 성공
assertThat(successCount.get()).isEqualTo(3)
```

## 샘플 데이터

애플리케이션 기동 시 Flyway가 자동으로 삽입합니다.

| 호텔 | 룸 타입 | 가격 | 재고 |
|------|---------|------|------|
| 시그니엘 서울 | 그랜드 디럭스 | 350,000원 | 10실/일 |
| 시그니엘 서울 | 프리미어 스위트 | 750,000원 | 5실/일 |
| 시그니엘 서울 | 로얄 스위트 | 1,500,000원 | 3실/일 |
| 롯데호텔 제주 | 스탠다드 오션뷰 | 200,000원 | 8실/일 |
| 롯데호텔 제주 | 디럭스 오션뷰 | 320,000원 | 4실/일 |

오늘 기준 30일간의 재고가 생성됩니다.

## ERD

```
hotel (1) ──── (N) room_type (1) ──── (N) inventory
                        │
                        └──── (N) reservation (N) ──── (1) guest
```

| 테이블 | 설명 |
|--------|------|
| `hotel` | 호텔 정보 |
| `room_type` | 객실 타입 (호텔에 종속) |
| `inventory` | 날짜별 재고 (unique: room_type_id + date) |
| `reservation` | 예약 (soft delete: CONFIRMED/CANCELLED) |
| `guest` | 투숙객 (email UNIQUE, 동일 이메일 시 재사용) |

## 추가 고려사항 (결제 단계 추가 시 향후 확장)

> 참고: [커머스 재고 시스템 설계](https://www.kyungyeon.dev/posts/132/)

### 1. 예약과 홀드 분리

현재는 예약 즉시 확정(`CONFIRMED`)하지만, 결제가 추가되면 **"예약 토큰 발급 → 결제 → 확정"** 2단계로 분리해야 합니다. 핵심은 **재고 차감을 짧고 빠른 경로(Hold)에서 먼저 수행**하고, 결제 완료 시 확정하는 것입니다.

- Hold 시 재고 선차감 + TTL(10분) 설정 → 만료 시 자동 복원
- 상태: `PENDING` → `CONFIRMED` / `EXPIRED`
- 병목이 되는 row에 락이 오래 걸리는 구조를 피해야 하므로, Hold 처리는 Redis 원자 연산으로 빠르게, DB 확정은 결제 완료 후 비동기로 수행

### 2. 멀티 인스턴스: In-Memory → Redis 전환

현재 `AtomicInteger` 카운터는 단일 서버 전용입니다. 다중 인스턴스에서는 Redis `DECRBY` + Lua 스크립트로 교체하되, `tryDecrement`/`increment` 인터페이스를 유지하면 교체 범위가 카운터 서비스 내부로 한정됩니다. Redis 장애 시 DB 직행 Fallback을 두면 정합성은 DB Lock이 보장합니다.

### 3. 동시성 병목 완화

재고 100개에 10,000건이 몰리면 단일 row `FOR UPDATE`가 병목이 됩니다.

- **버킷 샤딩**: 재고 row를 N개로 분할(예: 10개 x 10), 요청을 분산하여 락 경합 감소
- **대기열(Queue)**: 요청을 큐에 적재 후 순차 처리, DB 보호 확실하지만 실시간 응답 UX 설계 필요
- **Redis 선차감**: 현재 구현과 동일한 아이디어. 대부분을 Redis에서 걸러내고 DB 진입량 최소화

### 4. 중복 요청 방지 (다층 검증)

> 참고: [결제 중복 방지 사례](https://juyeongpark.tistory.com/182)

단일 레이어로는 동시 요청 시 검증~처리 사이 틈이 생깁니다. 방어를 계층화해야 합니다.

- **1차 — Redis 분산 락**: `SET NX + TTL(5s)`로 동일 요청 키에 대해 하나만 진입 허용. 미들웨어 레벨에서 중복 차단
- **2차 — Idempotency Key**: 클라이언트가 UUID를 헤더로 전달, 서버가 처리 여부를 확인하여 중복 요청 시 이전 응답 반환. DB `UNIQUE` 제약으로 최종 안전망
- **3차 — 서비스 레이어 검증**: 트랜잭션 내에서 상태·금액 등 비즈니스 규칙 재검증

### 5. 기타

- **Reconciliation**: 매일 `총재고 - 확정예약수량`을 검증하여 불일치 자동 보정
- **Read Replica**: 조회는 Replica, 변경은 Master로 분리

### 설계 원칙 요약

| 항목 | 현재 | 확장 시 |
|------|------|---------|
| 정합성 | DB Pessimistic Lock | 동일 (불변) |
| 성능 필터 | In-Memory 카운터 | Redis 카운터 |
| 예약 모델 | 즉시 확정 | Hold(선차감) → 결제 → 확정 |
| 병목 완화 | 단일 row Lock | 버킷 샤딩 / 대기열 |
| 중복 방지 | 없음 | Redis 분산 락 + Idempotency Key |


## AI 어시스턴트 설정

- `CLAUDE.md` — Claude Code 프로젝트 지침
- `.claude/skills/` — AI 코딩 규칙 및 패턴
