# Trip - 호텔 예약 API 서버

## 개발 원칙

**모든 코드 작성, 수정, 리뷰 시 다음 스킬들을 자동으로 따릅니다**:

### 필수 적용 스킬
- **@clean-code** — 클린코드 원칙 (SOLID, 명확한 네이밍, 함수 단일 책임)
- **@coding-conventions** — 패키지 구조, 레이어 규칙, Kotlin 컨벤션
- **@code-review** — 코드 수정/커밋/PR 전 자체 검토 체크리스트
  - 재고 관리 동시성 제어 검증
  - 트랜잭션 처리 및 롤백 확인
  - 에러 처리 완전성 검토
  - 테스트 커버리지 확인

### 도메인별 적용 스킬
- **@hotel-inventory-domain** — 재고 관련 코드 작성 시
- **@hotel-reservation-domain** — 예약 관련 코드 작성 시
- **@spring-boot-jpa-concurrency** — 동시성 제어 필요 시 (재고 차감/복원)

### 테스트 작성 시
- **@kotlin-spring-testing** — 단위/통합/Controller/Repository 테스트 패턴
- **@verification-loop** — 구현 완료 후 검증 프로세스

### API 개발 시
- **@api-design** — RESTful 설계, HTTP 상태 코드, 에러 응답 형식

### 데이터베이스 작업 시
- **@flyway-h2-patterns** — 마이그레이션 파일 작성/수정 시

## 빌드 & 실행

```bash
./gradlew build      # 빌드 (lint + 컴파일 + 테스트)
./gradlew bootRun    # 실행 (http://localhost:8080)
./gradlew test       # 테스트
./gradlew ktlintFormat  # lint 자동 수정
```

- H2 콘솔: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:tripdb`)
- Swagger UI: http://localhost:8080/swagger-ui.html

## 기술 스택
- Kotlin 1.9.25 + Spring Boot 3.2.5 + Java 21
- Spring Data JPA + H2 (in-memory) + Flyway
- SpringDoc OpenAPI (Swagger UI)
- ktlint (코드 포맷)
- Gradle Kotlin DSL + Version Catalog (`gradle/libs.versions.toml`)

## 코딩 컨벤션
> 상세 규칙은 `.claude/skills/coding-conventions` 참조

## 도메인 규칙
> 상세 규칙은 skills 참조

- 재고 모델/차감/복원/동시성 → `.claude/skills/hotel-inventory-domain`
- 예약 생명주기/상태 전이/검증/예외 → `.claude/skills/hotel-reservation-domain`
- Pessimistic Lock 패턴 (2중 동시성 제어) → `.claude/skills/spring-boot-jpa-concurrency`

## 금지 사항
- 하드코딩된 시크릿 금지 (application.properties 또는 환경변수 사용)
- 의존성 버전을 build.gradle.kts에 직접 하드코딩 금지 (`gradle/libs.versions.toml` 사용)

> 잠금/재고/JPQL 관련 금지 사항은 skills 참조: `hotel-inventory-domain`, `spring-boot-jpa-concurrency`

## 테스트 규칙
> 상세 패턴은 `.claude/skills/kotlin-spring-testing` 참조

- 동시성 테스트 필수 (재고 정합성 핵심 검증)

## 프로젝트 도구

### Skills (`.claude/skills/`)
- `clean-code` — 클린코드 원칙 (함수 설계, 네이밍, 에러 처리)
- `coding-conventions` — 패키지 구조, 레이어 규칙, 네이밍, 포맷팅
- `hotel-inventory-domain` — 재고 도메인 규칙
- `hotel-reservation-domain` — 예약 도메인 규칙
- `spring-boot-jpa-concurrency` — 동시성 제어 패턴
- `kotlin-spring-testing` — 테스트 패턴
- `flyway-h2-patterns` — DB 마이그레이션 패턴
- `api-design` — REST API 설계 패턴
- `verification-loop` — 빌드-테스트-검증 루프
- `code-review` — 코드 리뷰 체크리스트
- `test-coverage` — 테스트 커버리지 분석

### Commands (`.claude/commands/`)
워크플로우 참조 문서 (slash command 아님, 일반 명령으로 참조):
- `plan.md` — 구현 계획 수립 워크플로우
- `tdd.md` — TDD 기반 구현 가이드
- `code-review.md` — 코드 리뷰 프로세스
- `build-fix.md` — 빌드 에러 수정 절차
- `verify.md` — 전체 검증 체크리스트
- `test-coverage.md` → **Skills로 이동**
