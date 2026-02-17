# /test-coverage - 테스트 커버리지 분석

테스트 커버리지를 분석하고 누락된 테스트를 식별합니다.

## 워크플로우

### 1. 현재 테스트 현황 파악
- `src/test/` 디렉토리의 테스트 파일 목록 수집
- 각 테스트 클래스의 테스트 메서드 수 확인
- `./gradlew test` 실행하여 전체 통과 확인

### 2. 커버리지 분석

#### Service Layer
- [ ] 모든 public 메서드에 Unit 테스트
- [ ] 정상 케이스 + 예외 케이스
- [ ] 동시성 시나리오 (Integration)

#### Controller Layer
- [ ] 모든 API 엔드포인트에 MockMvc 테스트
- [ ] 성공 응답 + 에러 응답
- [ ] 입력 유효성 검사 테스트

#### Repository Layer
- [ ] 커스텀 쿼리 메서드 테스트
- [ ] 잠금 쿼리 동작 확인

#### Edge Cases
- [ ] null/빈값 처리
- [ ] 경계값 (재고 0, 1, 최대값)
- [ ] 잘못된 상태 전이
- [ ] 존재하지 않는 리소스
- [ ] 과거 날짜, 동일 날짜

### 3. 누락 테스트 보고

```
## 테스트 커버리지 보고서

### 현황
| 레이어 | 테스트 수 | 커버리지 |
|--------|----------|---------|
| Service | N | 예상 X% |
| Controller | N | 예상 X% |
| Repository | N | 예상 X% |

### 누락 테스트
1. [파일:메서드] - [누락 사유]
2. ...

### 권장 추가 테스트
1. [테스트명] - [검증 대상]
2. ...
```

## 커버리지 도구 (Kover)

```bash
# 커버리지 리포트 생성
./gradlew koverHtmlReport

# 리포트 위치: build/reports/kover/html/index.html
```

Kover 미설정 시 수동 분석:
1. `src/test/` 테스트 파일과 `src/main/` 소스 파일 매칭
2. 테스트되지 않은 public 메서드 목록 도출
3. 커버리지 추정: (테스트된 메서드 수 / 전체 public 메서드 수)

## $ARGUMENTS 옵션
- `service` - Service 레이어만 분석
- `controller` - Controller 레이어만 분석
- `concurrency` - 동시성 테스트만 분석
- (빈 값) - 전체 분석
