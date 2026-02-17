---
name: spring-boot-jpa-concurrency
description: Spring Boot + JPA 환경에서의 동시성 제어 패턴. Pessimistic Lock, Optimistic Lock, 트랜잭션 경계 설정.
---

# Spring Boot JPA 동시성 제어 패턴

## Pessimistic Lock (비관적 잠금)

SELECT ... FOR UPDATE로 행 수준 잠금을 획득하여 다른 트랜잭션의 동시 접근을 차단.

### Repository 정의

```kotlin
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT i FROM Inventory i WHERE i.roomType.id = :roomTypeId AND i.date >= :startDate AND i.date < :endDate ORDER BY i.date ASC")
fun findByRoomTypeIdAndDateRangeForUpdate(
    @Param("roomTypeId") roomTypeId: Long,
    @Param("startDate") startDate: LocalDate,
    @Param("endDate") endDate: LocalDate
): List<Inventory>
```

### 핵심 규칙

1. **ORDER BY 필수**: 데드락 방지를 위해 항상 일관된 순서로 잠금 획득
2. **잠금 타임아웃 설정**: `spring.jpa.properties.jakarta.persistence.lock.timeout=5000`
3. **트랜잭션 범위 최소화**: 잠금 보유 시간 최소화
4. **읽기 전용 쿼리에는 잠금 사용 금지**: 조회 API는 일반 쿼리 사용

### 동시성 시나리오

```
Thread A: BEGIN → SELECT FOR UPDATE (lock 획득) → 재고 확인 → 차감 → COMMIT (lock 해제)
Thread B: BEGIN → SELECT FOR UPDATE (대기) → ... → lock 획득 → 재고 확인 → 부족 → ROLLBACK
```

## Optimistic Lock (낙관적 잠금)

`@Version` 필드로 업데이트 시점에 충돌을 감지. Pessimistic Lock의 보조 안전장치.

### Entity 정의

```kotlin
@Entity
class Inventory(
    @Version
    @Column(nullable = false)
    var version: Long = 0
)
```

### 동작 방식

1. 엔티티 조회 시 version 값 읽음
2. JPA dirty checking으로 UPDATE 발생 시 `WHERE version = ?` 포함
3. 다른 트랜잭션이 먼저 수정했으면 `OptimisticLockException` 발생
4. version은 JPA가 자동 증가시킴 — 수동 변경 금지

## 트랜잭션 경계 설정

```kotlin
@Service
class ReservationService {
    @Transactional  // 쓰기 트랜잭션
    fun createReservation(request: CreateReservationRequest): ReservationResponse { ... }

    @Transactional(readOnly = true)  // 읽기 전용
    fun getReservation(id: Long): ReservationResponse { ... }
}
```

### 트랜잭션 규칙

- **`@Transactional`**: 쓰기 작업에만 사용
- **`@Transactional(readOnly = true)`**: 조회에 사용, Hibernate 최적화 활성화
- **서비스 레이어에서 선언**: 컨트롤러나 리포지토리에 선언하지 않음

## 예외 처리

| 예외 | HTTP 상태 | 의미 |
|------|----------|------|
| OptimisticLockException | 409 Conflict | 동시 수정 충돌 |
| PessimisticLockException | 503 Service Unavailable | 잠금 획득 타임아웃 |

## JPQL 안전 규칙

- JPQL에서 문자열 연결(String concatenation) 금지 — 파라미터 바인딩(`:paramName`) 사용
- 잘못된 예: `"WHERE i.date = '" + date + "'"` → SQL Injection 위험
- 올바른 예: `"WHERE i.date = :date"` + `@Param("date")`

## 체크리스트

- [ ] 재고 변경 시 Pessimistic Lock 사용
- [ ] ORDER BY로 일관된 잠금 순서 보장
- [ ] 잠금 타임아웃 설정 (5초)
- [ ] @Version 필드로 이중 안전장치
- [ ] 읽기 전용 API에는 잠금 미사용
- [ ] JPQL 파라미터 바인딩 사용 (문자열 연결 금지)
- [ ] 동시성 테스트로 정합성 검증