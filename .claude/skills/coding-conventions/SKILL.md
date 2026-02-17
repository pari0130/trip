---
name: coding-conventions
description: 프로젝트 코딩 컨벤션 — 패키지 구조, 레이어 규칙, 네이밍, 포맷팅
---

# 코딩 컨벤션

## 패키지 구조

```
com.trip.hotel.trip/
├── domain/
│   ├── entity/       JPA Entity (allOpen 플러그인 적용)
│   └── repository/   Spring Data JPA Repository
├── service/          비즈니스 로직, @Transactional 선언
├── controller/       REST API, @Valid 검증
├── dto/
│   ├── request/      요청 DTO (data class + Jakarta Validation)
│   └── response/     응답 DTO (불변 data class)
└── exception/        BusinessException 상속 계층, GlobalExceptionHandler
```

## 레이어 규칙

| 레이어 | 허용 | 금지 |
|--------|------|------|
| Controller | 요청/응답 변환, @Valid | 비즈니스 로직, @Transactional |
| Service | 비즈니스 로직, @Transactional | 직접 HTTP 응답 생성 |
| Repository | 데이터 접근, @Lock, @Query | 비즈니스 로직 |
| Entity | 도메인 로직 (차감/복원) | 외부 서비스 호출 |

## 네이밍 규칙

- **Entity**: 단수명사 (`Hotel`, `Reservation`)
- **Repository**: `{Entity}Repository`
- **Service**: `{Domain}Service` (`InventoryService`, `ReservationService`)
- **Controller**: `{Domain}Controller`
- **Request DTO**: `{Action}{Domain}Request` (`CreateReservationRequest`)
- **Response DTO**: `{Domain}Response` (`ReservationResponse`)
- **Exception**: `{원인}Exception` (`InsufficientInventoryException`)

## 언어 규칙

- 코드(클래스명, 변수명, 메서드명): 영어
- 주석, `@DisplayName`: 한국어 허용
- 에러 메시지: 한국어

## 포맷팅

- indent: tab (`.editorconfig` 참조)
- max line length: 120자
- ktlint 자동 적용 (`./gradlew ktlintFormat`)

## 의존성 관리

- 모든 버전: `gradle/libs.versions.toml`에서 관리
- `build.gradle.kts`에 버전 직접 하드코딩 금지
- 참조 방식: `libs.{library}`, `libs.plugins.{plugin}`, `libs.versions.{version}`
