package com.trip.hotel.domain.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest
@Transactional
class InventoryRepositoryTest {
	@Autowired
	private lateinit var inventoryRepository: InventoryRepository

	@Test
	@DisplayName("날짜 범위로 재고 조회 - 체크인(포함) ~ 체크아웃(미포함)")
	fun findByRoomTypeIdAndDateRange() {
		val checkInDate = LocalDate.now()
		val checkOutDate = LocalDate.now().plusDays(3)

		val inventories = inventoryRepository.findByRoomTypeIdAndDateRange(1L, checkInDate, checkOutDate)

		// 3박 = checkIn, checkIn+1, checkIn+2 → 3개 행
		assertEquals(3, inventories.size)
		assertTrue(inventories.all { it.date >= checkInDate && it.date < checkOutDate })
		// ORDER BY date ASC 확인
		assertEquals(inventories, inventories.sortedBy { it.date })
	}

	@Test
	@DisplayName("FOR UPDATE 잠금 쿼리 정상 동작")
	fun findByRoomTypeIdAndDateRangeForUpdate() {
		val checkInDate = LocalDate.now()
		val checkOutDate = LocalDate.now().plusDays(2)

		val inventories = inventoryRepository.findByRoomTypeIdAndDateRangeForUpdate(1L, checkInDate, checkOutDate)

		assertEquals(2, inventories.size)
		inventories.forEach { inventory ->
			assertEquals(10, inventory.totalQuantity)
			assertEquals(10, inventory.availableQuantity)
		}
	}

	@Test
	@DisplayName("존재하지 않는 룸타입 조회 시 빈 리스트")
	fun findByRoomTypeIdAndDateRange_noResult() {
		val inventories =
			inventoryRepository.findByRoomTypeIdAndDateRange(
				999L,
				LocalDate.now(),
				LocalDate.now().plusDays(1)
			)

		assertTrue(inventories.isEmpty())
	}
}
