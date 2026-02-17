---
name: flyway-h2-patterns
description: Flyway + H2 Database 마이그레이션 패턴 — 네이밍, H2 문법, ddl-auto=validate 조합
---

# Flyway + H2 Database 패턴

## 마이그레이션 파일 네이밍

경로: `src/main/resources/db/migration/`
형식: `V{버전}__{설명}.sql` (밑줄 2개)

```
V1__create_schema.sql
V2__insert_sample_data.sql
V3__add_index.sql
```

## ddl-auto=validate + Flyway 조합

```properties
spring.jpa.hibernate.ddl-auto=validate   # Hibernate는 검증만
spring.flyway.enabled=true               # Flyway가 스키마 관리
```

- Flyway: DDL 실행 → Hibernate: 엔티티-스키마 일치 검증
- 불일치 시 앱 시작 실패 → 빠른 오류 감지

## H2 전용 문법

```sql
-- 연속 숫자 생성 (PostgreSQL의 generate_series 대응)
SELECT x FROM SYSTEM_RANGE(0, 29) AS t(x);

-- 날짜 연산
SELECT DATEADD('DAY', x, CURRENT_DATE) FROM SYSTEM_RANGE(0, 29) AS t(x);

-- 30일치 재고 자동 생성
INSERT INTO inventory (room_type_id, date, total_quantity, available_quantity)
SELECT 1, DATEADD('DAY', x, CURRENT_DATE), 10, 10
FROM SYSTEM_RANGE(0, 29) AS t(x);
```

## 안전 규칙

- 이미 적용된 마이그레이션 파일 수정 금지
- 새 컬럼 추가 시 DEFAULT 값 지정
- 시드 데이터는 별도 마이그레이션 파일
