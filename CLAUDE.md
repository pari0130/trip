# Trip - 호텔 예약 API 서버

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
- Pessimistic/Optimistic Lock 패턴 → `.claude/skills/spring-boot-jpa-concurrency`

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

### Commands (`.claude/commands/`)
- `/plan` — 구현 계획 수립
- `/tdd` — TDD 기반 구현
- `/code-review` — 코드 리뷰
- `/build-fix` — 빌드 에러 수정
- `/verify` — 전체 검증
- `/test-coverage` — 테스트 커버리지 분석

### Agents (`.claude/agents/`)
- `planner` — 구현 계획: 무엇을/어떤 순서로 (Opus, read-only)
- `architect` — 설계 판단: 왜/트레이드오프 (Opus, read-only)
- `tdd-guide` — TDD 구현 (Sonnet)
- `code-reviewer` — 코드 리뷰 (Sonnet)
- `security-reviewer` — 보안 리뷰 (Sonnet)
- `build-error-resolver` — 빌드 에러 수정 (Sonnet)
