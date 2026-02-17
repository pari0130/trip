package com.trip.hotel.controller

import com.trip.hotel.dto.response.DailyAvailability
import com.trip.hotel.dto.response.InventoryResponse
import com.trip.hotel.exception.RoomTypeNotFoundException
import com.trip.hotel.service.InventoryService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(InventoryController::class)
class InventoryControllerTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@MockBean
	private lateinit var inventoryService: InventoryService

	private val checkInDate: LocalDate = LocalDate.now().plusDays(1)
	private val checkOutDate: LocalDate = LocalDate.now().plusDays(3)

	@Test
	@DisplayName("재고 조회 - 200 OK")
	fun getAvailability_200() {
		val response =
			InventoryResponse(
				roomTypeId = 1,
				roomTypeName = "그랜드 디럭스",
				checkInDate = checkInDate,
				checkOutDate = checkOutDate,
				dailyAvailabilities =
					listOf(
						DailyAvailability(date = checkInDate, totalQuantity = 10, availableQuantity = 10),
						DailyAvailability(date = checkInDate.plusDays(1), totalQuantity = 10, availableQuantity = 10)
					)
			)
		whenever(inventoryService.getAvailability(1L, checkInDate, checkOutDate)).thenReturn(response)

		mockMvc.perform(
			get("/api/v1/inventory")
				.param("roomTypeId", "1")
				.param("checkInDate", checkInDate.toString())
				.param("checkOutDate", checkOutDate.toString())
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.roomTypeId").value(1))
			.andExpect(jsonPath("$.roomTypeName").value("그랜드 디럭스"))
			.andExpect(jsonPath("$.dailyAvailabilities.length()").value(2))
	}

	@Test
	@DisplayName("존재하지 않는 룸 타입 - 404")
	fun getAvailability_404() {
		whenever(inventoryService.getAvailability(999L, checkInDate, checkOutDate))
			.thenThrow(RoomTypeNotFoundException(999L))

		mockMvc.perform(
			get("/api/v1/inventory")
				.param("roomTypeId", "999")
				.param("checkInDate", checkInDate.toString())
				.param("checkOutDate", checkOutDate.toString())
		)
			.andExpect(status().isNotFound)
	}

	@Test
	@DisplayName("날짜 범위 오류 - 400")
	fun getAvailability_400_invalidDateRange() {
		whenever(inventoryService.getAvailability(1L, checkOutDate, checkInDate))
			.thenThrow(IllegalArgumentException("체크인 날짜는 체크아웃 날짜보다 이전이어야 합니다."))

		mockMvc.perform(
			get("/api/v1/inventory")
				.param("roomTypeId", "1")
				.param("checkInDate", checkOutDate.toString())
				.param("checkOutDate", checkInDate.toString())
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.status").value(400))
	}

	@Test
	@DisplayName("필수 파라미터 누락 - 400")
	fun getAvailability_400_missingParam() {
		mockMvc.perform(
			get("/api/v1/inventory")
				.param("roomTypeId", "1")
		)
			.andExpect(status().isBadRequest)
	}
}
