---
name: api-design
description: REST API 설계 패턴 — 리소스 설계, HTTP 메서드, 상태 코드, 응답 형식, 에러 처리
---

# REST API 설계 패턴

## URL 구조

```
/api/v1/{resource}           # 컬렉션
/api/v1/{resource}/{id}      # 개별 리소스
/api/v1/{resource}/{id}/{action}  # 리소스 액션
```

### 이 프로젝트 API

| 메서드 | 엔드포인트 | 설명 | 상태코드 |
|--------|-----------|------|---------|
| GET | `/api/v1/inventory?roomTypeId=&checkInDate=&checkOutDate=` | 재고 조회 | 200 |
| POST | `/api/v1/reservations` | 예약 생성 | 201 |
| GET | `/api/v1/reservations/{id}` | 예약 조회 | 200 |
| PATCH | `/api/v1/reservations/{id}/cancel` | 예약 취소 | 200 |

## HTTP 상태 코드

| 코드 | 의미 | 사용처 |
|------|------|-------|
| 200 | OK | 조회/수정 성공 |
| 201 | Created | 리소스 생성 성공 |
| 400 | Bad Request | 유효성 검증 실패 |
| 404 | Not Found | 리소스 없음 |
| 409 | Conflict | 재고 부족, 상태 충돌 |
| 503 | Service Unavailable | 잠금 타임아웃 |

## 응답 형식

### 성공 응답 (예약)

```json
{
  "id": 1,
  "roomTypeId": 1,
  "roomTypeName": "그랜드 디럭스",
  "guestName": "홍길동",
  "guestEmail": "hong@example.com",
  "checkInDate": "2026-03-01",
  "checkOutDate": "2026-03-03",
  "numberOfRooms": 1,
  "status": "CONFIRMED"
}
```

### 에러 응답

```json
{
  "timestamp": "2026-03-01T10:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "재고가 부족합니다. roomTypeId=1, 부족 날짜: 2026-03-01"
}
```

## Request DTO + Jakarta Validation

```kotlin
data class CreateReservationRequest(
    @field:NotNull val roomTypeId: Long,
    @field:NotBlank val guestName: String,
    @field:Email val guestEmail: String,
    @field:FutureOrPresent val checkInDate: LocalDate,
    @field:Future val checkOutDate: LocalDate,
    @field:Min(1) val numberOfRooms: Int = 1
)
```

## 규칙

- 리소스명: 복수형 명사 (reservations, inventory)
- 동사 금지: URL에 동사 사용 금지 (create, delete 등)
- PATCH: 부분 수정 (cancel 같은 상태 변경)
- Content-Type: `application/json`
- 날짜 형식: ISO 8601 (`yyyy-MM-dd`)
