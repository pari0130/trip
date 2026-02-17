---
name: hotel-reservation-domain
description: 호텔 예약 도메인 규칙 — 예약 생명주기, 상태 전이, 검증 규칙, 예외 체계
---

# 호텔 예약 도메인 규칙

> 재고 제약조건, 동시성 제어는 `hotel-inventory-domain` skill 참조

## 예약 생명주기

```
CONFIRMED → CANCELLED (단방향, 재활성화 불가)
```

- **CONFIRMED**: 유효 상태. 재고 차감됨.
- **CANCELLED**: 소프트 삭제. 재고 복원됨.

## 상태 전이 규칙

- CONFIRMED → CANCELLED: 허용
- CANCELLED → CONFIRMED: **불가**
- CANCELLED → CANCELLED: **불가** (이미 취소 예외)

## 검증 규칙

| 필드 | 규칙 |
|------|------|
| roomTypeId | @NotNull |
| guestName | @NotBlank |
| guestEmail | @NotBlank, @Email |
| checkInDate | @NotNull, @FutureOrPresent |
| checkOutDate | @NotNull, @Future |
| numberOfRooms | @Min(1) |

## 트랜잭션 규칙

- 예약 생성: `@Transactional` — 재고 차감 + 예약 저장 원자적
- 예약 취소: `@Transactional` — 재고 복원 + 상태 변경 원자적
- 예약 조회: `@Transactional(readOnly = true)` — 잠금 없음

## 예외 체계

| 예외 | HTTP | 발생 조건 |
|------|------|----------|
| RoomTypeNotFoundException | 404 | 존재하지 않는 룸 타입 |
| ReservationNotFoundException | 404 | 존재하지 않는 예약 |
| InsufficientInventoryException | 409 | 재고 부족 |
| InvalidReservationStateException | 409 | 이미 취소된 예약 |
| OptimisticLockingFailureException | 409 | 동시 수정 충돌 |
| PessimisticLockingFailureException | 503 | 잠금 타임아웃 |
