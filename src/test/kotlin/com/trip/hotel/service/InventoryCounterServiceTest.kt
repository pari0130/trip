package com.trip.hotel.service

import com.trip.hotel.domain.entity.Hotel
import com.trip.hotel.domain.entity.Inventory
import com.trip.hotel.domain.entity.RoomType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class InventoryCounterServiceTest {
	private lateinit var counterService: InventoryCounterService
	private lateinit var roomType: RoomType
	private val baseDate: LocalDate = LocalDate.of(2026, 2, 18)

	@BeforeEach
	fun setUp() {
		counterService = InventoryCounterService()
		val hotel = Hotel(id = 1, name = "테스트 호텔", address = "서울")
		roomType = RoomType(id = 1, hotel = hotel, name = "디럭스", price = BigDecimal("100000"), maxOccupancy = 2)
	}

	@Test
	@DisplayName("initialize: Inventory 목록으로 카운터 세팅")
	fun initialize_setsCounters() {
		val inventories =
			listOf(
				Inventory(id = 1, roomType = roomType, date = baseDate, totalQuantity = 10, availableQuantity = 10),
				Inventory(id = 2, roomType = roomType, date = baseDate.plusDays(1), totalQuantity = 10, availableQuantity = 8)
			)

		counterService.initialize(inventories)

		assertEquals(10, counterService.getCount(1, baseDate))
		assertEquals(8, counterService.getCount(1, baseDate.plusDays(1)))
	}

	@Test
	@DisplayName("단일 날짜 차감 성공")
	fun tryDecrement_singleDate_success() {
		initializeCounter(10)

		val result = counterService.tryDecrement(1, listOf(baseDate), 1)

		assertTrue(result)
		assertEquals(9, counterService.getCount(1, baseDate))
	}

	@Test
	@DisplayName("다중 날짜 차감 성공")
	fun tryDecrement_multipleDates_success() {
		initializeCounter(10)
		val dates = listOf(baseDate, baseDate.plusDays(1))

		val result = counterService.tryDecrement(1, dates, 2)

		assertTrue(result)
		assertEquals(8, counterService.getCount(1, baseDate))
		assertEquals(8, counterService.getCount(1, baseDate.plusDays(1)))
	}

	@Test
	@DisplayName("중간 날짜 재고 부족 시 전체 롤백")
	fun tryDecrement_midwayFailure_rollbacksAll() {
		val inventories =
			listOf(
				Inventory(id = 1, roomType = roomType, date = baseDate, totalQuantity = 10, availableQuantity = 5),
				Inventory(id = 2, roomType = roomType, date = baseDate.plusDays(1), totalQuantity = 10, availableQuantity = 1)
			)
		counterService.initialize(inventories)

		val dates = listOf(baseDate, baseDate.plusDays(1))
		val result = counterService.tryDecrement(1, dates, 3)

		assertFalse(result)
		assertEquals(5, counterService.getCount(1, baseDate))
		assertEquals(1, counterService.getCount(1, baseDate.plusDays(1)))
	}

	@Test
	@DisplayName("미등록 날짜 차감 시도 시 롤백")
	fun tryDecrement_unregisteredDate_rollbacks() {
		initializeCounter(10)
		val unregisteredDate = baseDate.plusDays(99)
		val dates = listOf(baseDate, unregisteredDate)

		val result = counterService.tryDecrement(1, dates, 1)

		assertFalse(result)
		assertEquals(10, counterService.getCount(1, baseDate))
		assertNull(counterService.getCount(1, unregisteredDate))
	}

	@Test
	@DisplayName("increment: 차감 후 복원 시 원래 값")
	fun increment_restoresOriginalValue() {
		initializeCounter(10)
		val dates = listOf(baseDate, baseDate.plusDays(1))

		counterService.tryDecrement(1, dates, 3)
		assertEquals(7, counterService.getCount(1, baseDate))

		counterService.increment(1, dates, 3)
		assertEquals(10, counterService.getCount(1, baseDate))
		assertEquals(10, counterService.getCount(1, baseDate.plusDays(1)))
	}

	@Test
	@DisplayName("동시성: 10스레드 재고 5개 → 정확히 5개 성공")
	fun tryDecrement_concurrent_exactSuccessCount() {
		initializeCounter(5)
		val threadCount = 10
		val dates = listOf(baseDate, baseDate.plusDays(1))
		val latch = CountDownLatch(threadCount)
		val successCount = AtomicInteger(0)
		val executor = Executors.newFixedThreadPool(threadCount)

		repeat(threadCount) {
			executor.submit {
				try {
					if (counterService.tryDecrement(1, dates, 1)) {
						successCount.incrementAndGet()
					}
				} finally {
					latch.countDown()
				}
			}
		}
		latch.await()
		executor.shutdown()

		assertEquals(5, successCount.get())
		assertEquals(0, counterService.getCount(1, baseDate))
		assertEquals(0, counterService.getCount(1, baseDate.plusDays(1)))
	}

	private fun initializeCounter(availableQuantity: Int) {
		val inventories =
			listOf(
				Inventory(id = 1, roomType = roomType, date = baseDate, totalQuantity = 10, availableQuantity = availableQuantity),
				Inventory(
					id = 2,
					roomType = roomType,
					date = baseDate.plusDays(1),
					totalQuantity = 10,
					availableQuantity = availableQuantity
				)
			)
		counterService.initialize(inventories)
	}
}
