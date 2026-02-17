---
name: clean-code
description: 클린코드 원칙 — 함수 설계, 네이밍, 추상화 수준, 에러 처리, 코드 냄새 감지
---

# 클린코드 원칙

> "Code is clean if it can be read, and enhanced by a developer other than its original author." — Grady Booch

## 네이밍

- **의도를 드러낸다** — `d`(X) → `daysSinceCreation`(O)
- **오해를 유발하지 않는다** — Map인데 `accountList`(X) → `accountMap`(O)
- **의미 있는 구분** — `ProductData` vs `ProductInfo` 같은 모호한 구분 금지
- **검색 가능한 이름** — `5`(X) → `MAX_RETRY_COUNT = 5`(O)
- **클래스명: 명사** — `Reservation`, `Inventory`, `RoomType`
- **메서드명: 동사** — `findRoomType`, `cancelReservation`, `decreaseQuantity`
- **불리언: is/has/can** — `isAvailable`, `hasSufficientInventory`
- **약어 지양** — `qty`(X) → `quantity`(O), `inv`(X) → `inventory`(O)

## 함수

- **한 가지 일만 한다** — 함수명으로 설명되는 하나의 작업만 수행
- **작게 유지** — 20줄 이내 권장, 인덴트 2단계 이내
- **추상화 수준 통일** — 한 함수 안에 고수준/저수준 로직 혼재 금지
- **인자 최소화** — 0~2개 이상적, 3개 이상이면 객체로 묶기
- **부수 효과 없음** — 함수명에 드러나지 않는 상태 변경 금지

```kotlin
// Bad: 여러 일을 하고, 추상화 수준 불일치
fun validateAndSaveReservation(request: Request): Reservation {
    if (request.checkInDate.isAfter(request.checkOutDate)) throw ...
    val inventories = inventoryRepository.findForUpdate(...)
    inventories.forEach { it.decrease(request.numberOfRooms) }
    return reservationRepository.save(...)
}

// Good: 한 가지 추상화 수준, 각 단계를 분리
fun createReservation(request: Request): Reservation {
    val roomType = findRoomType(request.roomTypeId)
    val inventories = lockInventories(roomType, request.checkInDate, request.checkOutDate)
    decreaseInventory(inventories, request.numberOfRooms)
    return saveReservation(request, roomType)
}
```

## 주석

- **나쁜 코드에 주석 달지 말고 코드를 개선** — 주석은 표현 실패의 신호
- **What/How 주석 금지** — 코드 자체로 설명
- **Why만 주석** — 왜 이런 결정을 했는지만 기록
- **허용되는 주석** — 법적 고지, TODO, 외부 라이브러리 제약 설명
- **거짓말하는 주석 삭제** — 코드와 동기화되지 않은 주석은 해악

```kotlin
// Bad: What을 설명
// 재고를 차감한다
inventory.decreaseAvailableQuantity(quantity)

// Good: Why를 설명
// 데드락 방지를 위해 날짜 오름차순으로 잠금 획득
@Query("... ORDER BY i.date ASC")
```

## 포맷팅

- **신문 기사 규칙** — 파일 상단에 고수준 개념, 하단에 세부 구현
- **수직 밀도** — 관련 코드는 가까이, 무관한 코드는 빈 줄로 분리
- **변수 선언은 사용 위치 근처** — 선언과 사용 사이 거리 최소화
- **호출하는 함수 위, 호출되는 함수 아래** — 위에서 아래로 읽히도록

## 객체와 데이터 구조

- **데이터 추상화** — 구현을 인터페이스 뒤에 숨기기
- **디미터 법칙** — `a.getB().getC().doSomething()` 체이닝 금지
- **DTO는 순수 데이터** — 로직 없이 데이터만 운반

```kotlin
// Bad: 디미터 법칙 위반
val price = reservation.roomType.hotel.basePrice

// Good: 필요한 정보만 직접 접근
val price = reservation.roomTypePrice
```

## 에러 처리

- **반환 코드 대신 예외** — `null` 반환(X) → 예외 던지기(O)
- **구체적 예외** — `RuntimeException`(X) → `InsufficientInventoryException`(O)
- **예외에 맥락 포함** — 어떤 날짜에 얼마나 부족한지 메시지에 담기
- **null 반환 금지** — `Optional` 또는 예외 사용
- **null 전달 금지** — NullPointerException 유발
- **early return** — 실패 조건을 먼저 검사하고 빠르게 반환

```kotlin
// Bad: 깊은 중첩 + null 반환
fun cancel(id: Long) {
    val reservation = repository.findById(id).orElse(null)
    if (reservation != null) {
        if (reservation.status == CONFIRMED) {
            // 취소 로직...
        } else { throw InvalidReservationStateException(...) }
    } else { throw ReservationNotFoundException(id) }
}

// Good: early return + 명확한 예외
fun cancel(id: Long) {
    val reservation = repository.findById(id)
        .orElseThrow { ReservationNotFoundException(id) }
    if (reservation.status != CONFIRMED) {
        throw InvalidReservationStateException(id, reservation.status)
    }
    // 취소 로직...
}
```

## 클래스

- **단일 책임 원칙(SRP)** — 변경 이유가 하나
- **작게 유지** — 200줄 이내 권장
- **응집도 높게** — 모든 메서드가 인스턴스 변수를 사용
- **하향식 서사** — 위에서 아래로 읽히도록 배치

```kotlin
// 검증은 DTO, 도메인 로직은 Entity, 변환은 Response에 위임
class Inventory {
    fun decreaseAvailableQuantity(quantity: Int) { ... }
    fun increaseAvailableQuantity(quantity: Int) { ... }
}
```

## 코드 냄새

| 냄새 | 증상 | 대응 |
|------|------|------|
| 경직성 | 하나 바꾸면 여러 곳 수정 | 인터페이스로 분리 |
| 취약성 | 수정 시 관련 없는 곳 깨짐 | 의존성 방향 정리 |
| 불필요한 복잡성 | 현재 불필요한 추상화 | YAGNI 적용, 삭제 |
| 불필요한 반복 | 동일 로직 복사 | 공통 함수 추출 |
| 불투명성 | 읽기 어려운 코드 | 네이밍 개선, 분리 |

## Kotlin 특화

- **`?.let` / `?:` 활용** — null 처리를 간결하게
- **data class 불변성** — DTO는 `val`만 사용
- **확장 함수** — 유틸성 로직은 확장 함수로 표현
- **when 표현식** — 다중 분기는 `if-else` 대신 `when`
- **매직 넘버 → 상수** — `5000`(X) → `LOCK_TIMEOUT_MS = 5000`(O)

## 체크리스트

- [ ] 함수가 20줄 미만이고 한 가지만 하는가
- [ ] 모든 이름이 의도를 드러내고 검색 가능한가
- [ ] 주석 없이 코드만으로 의도가 전달되는가
- [ ] 함수 인자가 3개 미만인가
- [ ] early return으로 중첩을 줄였는가
- [ ] null 반환/전달 없이 예외 또는 Optional을 사용하는가
- [ ] 예외에 충분한 맥락이 포함되는가
- [ ] 디미터 법칙을 위반하는 체이닝이 없는가
- [ ] 코드 냄새(반복, 불필요한 복잡성)가 없는가
