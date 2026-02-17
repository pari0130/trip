---
name: hotel-inventory-domain
description: 호텔 재고 도메인 규칙 — 재고 모델, 제약조건, 차감/복원 패턴, 동시성 제어
---

# 호텔 재고 도메인 규칙

## 재고 모델

- **날짜별 x 룸타입별 매트릭스** 구조
- `UNIQUE(room_type_id, date)` — 중복 불가
- `availableQuantity >= 0` — 음수 불가
- `availableQuantity <= totalQuantity` — 초과 불가

## 날짜 범위 규칙

- **checkInDate(포함) ~ checkOutDate(미포함)**
- SQL: `WHERE date >= checkInDate AND date < checkOutDate`
- **사전 검증**: `require(checkInDate.isBefore(checkOutDate))` — Service 레이어에서 검증
- **날짜 계산**: `ChronoUnit.DAYS.between()` 사용 필수 (`Period.days`는 월 경계에서 오류 발생)

## 차감 규칙

- 모든 날짜에 `availableQuantity >= 요청 수량` 검증 후 차감
- 하나라도 부족하면 **전체 실패** (부분 차감 없음)
- 날짜 범위에 해당하는 재고 행 누락 시 예외

```kotlin
fun decreaseAvailableQuantity(quantity: Int) {
    if (availableQuantity < quantity) {
        throw IllegalStateException("재고 부족: date=$date, 요청=$quantity, 잔여=$availableQuantity")
    }
    availableQuantity -= quantity
}
```

## 복원 규칙

- totalQuantity 초과 방지: `min(복원값, totalQuantity)`

```kotlin
fun increaseAvailableQuantity(quantity: Int) {
    availableQuantity = minOf(availableQuantity + quantity, totalQuantity)
}
```

## 동시성 제어 패턴

| 제어 | 역할 | 구현 |
|------|------|------|
| Pessimistic Lock (주) | 동시 접근 직렬화 | `@Lock(PESSIMISTIC_WRITE)` + `ORDER BY date ASC` |
| Optimistic Lock (보조) | 추가 안전장치 | `@Version` 필드 |

- `ORDER BY date ASC` 필수 — 데드락 방지 (잠금 순서 일관)
- 재고 변경 시 반드시 `findByRoomTypeIdAndDateRangeForUpdate` 사용
- 조회만 할 때는 잠금 없이 `@Transactional(readOnly = true)`
