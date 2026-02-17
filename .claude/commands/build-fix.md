# /build-fix - 빌드 에러 수정

빌드 에러를 진단하고 최소 변경으로 수정합니다.

## 워크플로우

### 1. 빌드 실행 및 에러 수집
```bash
./gradlew build 2>&1
```

### 2. 에러 분류
에러 메시지를 파일별로 그룹화하고 의존성 순서로 정렬:
- import/타입 에러 먼저
- 로직 에러 나중

### 3. 수정 (최소 변경 원칙)

| 에러 유형 | 수정 방법 |
|-----------|----------|
| Unresolved reference | import 추가 또는 의존성 확인 |
| Type mismatch | 타입 변환 또는 수정 |
| Missing override | override 키워드 추가 |
| Null safety | ?. 또는 !! 또는 null 체크 |
| Missing dependency | build.gradle.kts에 추가 |
| Flyway 실패 | SQL 문법 확인 |

### 4. 검증
```bash
./gradlew build 2>&1
```

에러 하나 수정 후 즉시 빌드 재실행. 새 에러 발생 시 원복.

## 규칙
- 에러와 관련 없는 코드 수정 금지
- 아키텍처 변경 금지
- 로직 흐름 변경 금지
- 수정 전후 비교 출력

## 복구 옵션
```bash
./gradlew clean build              # 캐시 클리어
./gradlew --stop && ./gradlew build  # 데몬 재시작
```
