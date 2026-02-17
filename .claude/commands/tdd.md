# /tdd - TDD 기반 구현

$ARGUMENTS 를 TDD 방식으로 구현합니다.

## 워크플로우

### 1. RED - 실패하는 테스트 작성
- $ARGUMENTS 의 기대 동작을 테스트로 정의
- given/when/then 패턴 사용
- `@DisplayName`에 한국어 설명 포함
- `./gradlew test` 실행하여 실패 확인

### 2. GREEN - 테스트를 통과시키는 최소 구현
- 테스트가 통과하는 최소한의 코드만 작성
- 기존 프로젝트 패턴 준수
- `./gradlew test` 실행하여 통과 확인

### 3. REFACTOR - 코드 개선
- 테스트 통과 상태 유지하며 코드 정리
- 중복 제거, 네이밍 개선
- `./gradlew test` 실행하여 여전히 통과 확인

## 테스트 유형별 패턴

### Unit Test (Service)
```kotlin
@ExtendWith(MockitoExtension::class)
class XxxServiceTest {
    @Mock lateinit var repository: XxxRepository
    @InjectMocks lateinit var service: XxxService
}
```

### Integration Test (동시성)
```kotlin
@SpringBootTest
class XxxConcurrencyTest {
    // ExecutorService + CountDownLatch + AtomicInteger
}
```

### Controller Test
```kotlin
@WebMvcTest(XxxController::class)
class XxxControllerTest {
    @Autowired lateinit var mockMvc: MockMvc
    @MockBean lateinit var service: XxxService
}
```

## 규칙
- 반드시 테스트 먼저 작성
- 각 단계 후 `./gradlew test` 실행
- Edge case 포함: null, 빈값, 경계값, 동시성
- 테스트 실패 시 구현 수정, 테스트 수정 금지
