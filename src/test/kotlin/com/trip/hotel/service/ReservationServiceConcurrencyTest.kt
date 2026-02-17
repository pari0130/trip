package com.trip.hotel.service

import com.trip.hotel.domain.repository.InventoryRepository
import com.trip.hotel.dto.request.CreateReservationRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class ReservationServiceConcurrencyTest {
	@Autowired
	private lateinit var reservationService: ReservationService

	@Autowired
	private lateinit var inventoryRepository: InventoryRepository

	@Autowired
	private lateinit var inventoryCounterService: InventoryCounterService

	@Test
	@DisplayName("10개 스레드 동시 예약 - 재고만큼만 성공")
	fun concurrentReservation_exactInventoryCount() {
		// given: 그랜드 디럭스 (roomTypeId=1) 재고 10실
		val threadCount = 15
		val checkInDate = LocalDate.now()
		val checkOutDate = LocalDate.now().plusDays(2)
		val request =
			CreateReservationRequest(
				roomTypeId = 1,
				guestName = "테스트",
				guestEmail = "test@test.com",
				checkInDate = checkInDate,
				checkOutDate = checkOutDate,
				numberOfRooms = 1
			)

		val executorService = Executors.newFixedThreadPool(threadCount)
		val latch = CountDownLatch(threadCount)
		val successCount = AtomicInteger(0)
		val failCount = AtomicInteger(0)

		// when
		repeat(threadCount) {
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

		// then: 재고 10개 → 정확히 10개 성공, 5개 실패
		assertEquals(threadCount, successCount.get() + failCount.get())
		assertEquals(10, successCount.get())
		assertEquals(5, failCount.get())

		// 재고 0 확인
		val inventories = inventoryRepository.findByRoomTypeIdAndDateRange(1L, checkInDate, checkOutDate)
		inventories.forEach { inventory ->
			assertEquals(0, inventory.availableQuantity)
		}
	}

	@Test
	@DisplayName("재고 3개인 룸타입에 10개 동시 요청 - 정확히 3개만 성공")
	fun concurrentReservation_preventOverselling() {
		// given: 로얄 스위트 (roomTypeId=3) 재고 3실
		val threadCount = 10
		val checkInDate = LocalDate.now()
		val checkOutDate = LocalDate.now().plusDays(1)
		val request =
			CreateReservationRequest(
				roomTypeId = 3,
				guestName = "테스트",
				guestEmail = "test@test.com",
				checkInDate = checkInDate,
				checkOutDate = checkOutDate,
				numberOfRooms = 1
			)

		val executorService = Executors.newFixedThreadPool(threadCount)
		val latch = CountDownLatch(threadCount)
		val successCount = AtomicInteger(0)
		val failCount = AtomicInteger(0)

		// when
		repeat(threadCount) {
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

		// then: 재고 3개 → 정확히 3개만 성공
		assertEquals(3, successCount.get())
		assertEquals(7, failCount.get())
	}

	@Test
	@DisplayName("동시 예약 후 카운터-DB 재고 일치 검증")
	fun concurrentReservation_counterMatchesDb() {
		// given: 스탠다드 오션뷰 (roomTypeId=4) 재고 8실
		val threadCount = 12
		val checkInDate = LocalDate.now()
		val checkOutDate = LocalDate.now().plusDays(1)
		val request =
			CreateReservationRequest(
				roomTypeId = 4,
				guestName = "테스트",
				guestEmail = "test@test.com",
				checkInDate = checkInDate,
				checkOutDate = checkOutDate,
				numberOfRooms = 1
			)

		val executorService = Executors.newFixedThreadPool(threadCount)
		val latch = CountDownLatch(threadCount)
		val successCount = AtomicInteger(0)

		// when
		repeat(threadCount) {
			executorService.submit {
				try {
					reservationService.createReservation(request)
					successCount.incrementAndGet()
				} catch (_: Exception) {
				} finally {
					latch.countDown()
				}
			}
		}
		latch.await()
		executorService.shutdown()

		// then: 카운터와 DB 재고가 일치해야 함
		assertEquals(8, successCount.get())

		val inventories = inventoryRepository.findByRoomTypeIdAndDateRange(4L, checkInDate, checkOutDate)
		inventories.forEach { inventory ->
			val counterValue = inventoryCounterService.getCount(4, inventory.date)
			assertEquals(
				inventory.availableQuantity,
				counterValue,
				"날짜 ${inventory.date}: DB 재고(${inventory.availableQuantity})와 카운터($counterValue)가 불일치"
			)
		}
	}
}
