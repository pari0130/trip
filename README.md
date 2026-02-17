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

### 3. 예약 조회

- `GET /api/v1/reservations/{id}`
- 잠금 없이 단순 조회

### 4. 예약 취소

- `PATCH /api/v1/reservations/{id}/cancel`
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
| 409 | 동시 수정 충돌 (@Version 불일치) | `ObjectOptimisticLockingFailureException` |
| 500 | 예상치 못한 서버 오류 | `Exception` (fallback) |
| 503 | 잠금 타임아웃 (동시 접근 과부하) | `PessimisticLockException` |

> 상세 API 스펙은 Swagger UI 참조: http://localhost:8080/swagger-ui.html

## 설계 시 주요 고려 사항

### 1. 동시 예약 시 재고 정합성

재고는 `totalQuantity`(전체 객실 수)와 `availableQuantity`(잔여 객실 수)로 관리됩니다. 예약 시 `availableQuantity`를 차감하고, 취소 시 복원합니다(`totalQuantity`를 초과하지 않도록 상한 제한).

여러 사용자가 같은 룸 타입의 같은 날짜에 동시에 예약하면, 재고가 음수가 되거나 초과 판매될 수 있습니다.

```
재고 3개 남은 상태에서 10명이 동시에 1실 예약 요청

Thread A: 재고 3 확인 → 차감 → 재고 2 (성공)
Thread B: 재고 2 확인 → 차감 → 재고 1 (성공)
Thread C: 재고 1 확인 → 차감 → 재고 0 (성공)
Thread D: 재고 0 확인 → 부족 → 실패
...
Thread J: 대기 → 재고 0 확인 → 부족 → 실패

결과: 정확히 3개만 성공, 7개 실패, 재고 = 0 (음수 불가)
```

#### 이중 잠금 전략

| 제어 방식 | 역할 | 구현 |
|-----------|------|------|
| **Pessimistic Lock** (주) | 동시 접근을 직렬화하여 순차 처리 | `SELECT ... FOR UPDATE` + `ORDER BY date` |
| **Optimistic Lock** (보조) | Pessimistic Lock을 우회하는 경로에 대한 안전장치 | `@Version` 컬럼 |

- **PESSIMISTIC_WRITE**: JPA의 `@Lock(LockModeType.PESSIMISTIC_WRITE)`를 사용하며, 이는 `SELECT ... FOR UPDATE` SQL로 변환됩니다. 잠금을 획득한 트랜잭션이 커밋/롤백될 때까지 다른 트랜잭션은 해당 행의 읽기/쓰기 모두 대기합니다. 잠금 대기 시간은 `spring.jpa.properties.jakarta.persistence.lock.timeout=5000` (5초)으로 설정되어 있으며, 초과 시 `PessimisticLockException` → 503 응답을 반환합니다.
- **@Version을 추가한 이유**: 현재 재고 변경은 모두 `FOR UPDATE`를 거치므로 `@Version` 충돌이 발생할 일은 거의 없습니다. 하지만 향후 Pessimistic Lock 없이 재고를 수정하는 코드가 추가될 경우, `@Version`이 없으면 동시 수정이 조용히 덮어쓰기(lost update)됩니다. `@Version`은 이를 예외로 감지하는 방어적 안전장치이며, UPDATE에 `AND version=?` 조건이 추가되는 정도의 무시 가능한 비용입니다.
- **데드락 방지**: 재고 행을 잠글 때 반드시 `ORDER BY date ASC`로 잠금 순서를 일관되게 유지합니다.

### 2. 트랜잭션과 롤백

모든 비즈니스 예외는 `RuntimeException`을 상속하므로, `@Transactional` 내에서 예외 발생 시 **자동 롤백**됩니다.

```
RuntimeException
  └── BusinessException (abstract)
        ├── RoomTypeNotFoundException (404)
        ├── ReservationNotFoundException (404)
        ├── InsufficientInventoryException (409)
        └── InvalidReservationStateException (409)
```

- **All or Nothing**: 3박 예약 시 재고 차감 중 2일차에서 실패하면, 1일차 차감도 함께 롤백
- JPA의 dirty checking은 트랜잭션 커밋 시점(flush)에 UPDATE 쿼리를 실행하므로, 예외 발생 시 flush 자체가 일어나지 않아 DB에는 아무것도 반영되지 않음
- Optimistic Lock 충돌(`ObjectOptimisticLockingFailureException`)도 `RuntimeException` 계열이므로 자동 롤백 대상

### 3. 예약 생명주기

```
CONFIRMED ──취소──→ CANCELLED (단방향, 재활성화 불가)
```

- 취소는 soft delete 방식으로 `status`를 변경하고 재고를 복원합니다.
- 이미 취소된 예약에 대한 재취소는 409 에러를 반환합니다.

## 프로젝트 구조

```
src/main/kotlin/com/trip/hotel/
├── TripApplication.kt
├── domain/
│   ├── entity/          Hotel, RoomType, Inventory, Reservation, ReservationStatus
│   └── repository/      HotelRepository, RoomTypeRepository, InventoryRepository, ReservationRepository
├── service/             InventoryService, ReservationService
├── config/              OpenApiConfig
├── controller/          InventoryController, ReservationController
├── dto/
│   ├── request/         CreateReservationRequest
│   └── response/        InventoryResponse, ReservationResponse, ErrorResponse
└── exception/           BusinessException, GlobalExceptionHandler + 구체 예외 클래스

src/main/resources/
├── application.properties
└── db/migration/
    ├── V1__create_schema.sql      DDL (hotel, room_type, inventory, reservation)
    └── V2__insert_sample_data.sql  시드 데이터 (호텔 2, 룸타입 5, 30일 재고)
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
                        └──── (N) reservation
```

| 테이블 | 설명 |
|--------|------|
| `hotel` | 호텔 정보 |
| `room_type` | 객실 타입 (호텔에 종속) |
| `inventory` | 날짜별 재고 (unique: room_type_id + date) |
| `reservation` | 예약 (soft delete: CONFIRMED/CANCELLED) |
