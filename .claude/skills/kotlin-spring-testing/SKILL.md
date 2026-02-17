---
name: kotlin-spring-testing
description: Kotlin + Spring Boot 테스트 패턴 — Unit, Integration, MockMvc, 동시성 테스트
---

# Kotlin + Spring Boot 테스트 패턴

## Unit 테스트 (Mockito)

```kotlin
@ExtendWith(MockitoExtension::class)
class ReservationServiceTest {
    @Mock private lateinit var reservationRepository: ReservationRepository
    @InjectMocks private lateinit var reservationService: ReservationService

    @Test
    @DisplayName("예약 생성 성공")
    fun createReservation_success() {
        // given - whenever()로 Mock 설정
        // when - 서비스 메서드 호출
        // then - assertEquals, verify
    }
}
```

의존성: `testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")`

## Integration 테스트 (동시성)

```kotlin
@SpringBootTest
class ConcurrencyTest {
    @Test
    fun concurrentReservation() {
        val threadCount = 10
        val executorService = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        repeat(threadCount) { i ->
            executorService.submit {
                try {
                    reservationService.createReservation(request)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        executorService.shutdown()
        // 검증: successCount + failCount == threadCount
    }
}
```

## Controller 테스트 (MockMvc)

```kotlin
@WebMvcTest(ReservationController::class)
class ReservationControllerTest {
    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper
    @MockBean private lateinit var reservationService: ReservationService

    @Test
    fun createReservation_201() {
        mockMvc.perform(post("/api/v1/reservations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
    }
}
```

**주의**: Spring Boot 3.2.x에서 `@MockBean`은 `org.springframework.boot.test.mock.mockito.MockBean`

## Repository 테스트

```kotlin
@SpringBootTest
@Transactional  // 테스트 후 자동 롤백
class InventoryRepositoryTest {
    @Autowired private lateinit var inventoryRepository: InventoryRepository
}
```

## 테스트 네이밍

```
메서드명_시나리오  (예: createReservation_success, cancelReservation_alreadyCancelled)
```

## 빌드 명령

```bash
./gradlew test                           # 전체 테스트
./gradlew test --tests "*ConcurrencyTest" # 특정 테스트
./gradlew test --info                     # 상세 로그
```

## 체크리스트

- [ ] 서비스: 성공/실패/예외 경로 모두 커버
- [ ] 동시성: ExecutorService + CountDownLatch + AtomicInteger
- [ ] 컨트롤러: 각 HTTP 상태코드별 테스트
- [ ] @DisplayName으로 한국어 설명
- [ ] 80%+ 커버리지 목표
