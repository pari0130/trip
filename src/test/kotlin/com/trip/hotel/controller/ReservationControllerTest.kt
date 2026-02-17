package com.trip.hotel.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.trip.hotel.domain.entity.ReservationStatus
import com.trip.hotel.dto.request.CreateReservationRequest
import com.trip.hotel.dto.response.ReservationResponse
import com.trip.hotel.exception.InsufficientInventoryException
import com.trip.hotel.exception.InvalidReservationStateException
import com.trip.hotel.exception.ReservationNotFoundException
import com.trip.hotel.service.ReservationService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(ReservationController::class)
class ReservationControllerTest {
	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var objectMapper: ObjectMapper

	@MockBean
	private lateinit var reservationService: ReservationService

	private val checkInDate: LocalDate = LocalDate.now().plusDays(1)
	private val checkOutDate: LocalDate = LocalDate.now().plusDays(3)
	private val now: LocalDateTime = LocalDateTime.now()

	private fun sampleResponse() =
		ReservationResponse(
			id = 1, roomTypeId = 1, roomTypeName = "그랜드 디럭스",
			guestName = "테스트유저1", guestEmail = "testuser1@test.com",
			checkInDate = checkInDate, checkOutDate = checkOutDate,
			numberOfRooms = 1, status = ReservationStatus.CONFIRMED,
			createdAt = now, updatedAt = now
		)

	@Test
	@DisplayName("예약 생성 - 201 Created")
	fun createReservation_201() {
		val request =
			CreateReservationRequest(
				roomTypeId = 1,
				guestName = "테스트유저1",
				guestEmail = "testuser1@test.com",
				checkInDate = checkInDate,
				checkOutDate = checkOutDate
			)
		whenever(reservationService.createReservation(any())).thenReturn(sampleResponse())

		mockMvc.perform(
			post("/api/v1/reservations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.guestName").value("테스트유저1"))
			.andExpect(jsonPath("$.status").value("CONFIRMED"))
	}

	@Test
	@DisplayName("유효성 검증 실패 - 400 Bad Request")
	fun createReservation_400_validation() {
		val invalidRequest =
			mapOf(
				"roomTypeId" to 1,
				"guestName" to "",
				"guestEmail" to "invalid-email",
				"checkInDate" to checkInDate.toString(),
				"checkOutDate" to checkOutDate.toString()
			)

		mockMvc.perform(
			post("/api/v1/reservations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest))
		)
			.andExpect(status().isBadRequest)
	}

	@Test
	@DisplayName("재고 부족 - 409 Conflict")
	fun createReservation_409_insufficientInventory() {
		whenever(reservationService.createReservation(any()))
			.thenThrow(InsufficientInventoryException(1L, checkInDate, 5, 3))

		val request =
			CreateReservationRequest(
				roomTypeId = 1,
				guestName = "테스트유저1",
				guestEmail = "testuser1@test.com",
				checkInDate = checkInDate,
				checkOutDate = checkOutDate,
				numberOfRooms = 5
			)

		mockMvc.perform(
			post("/api/v1/reservations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
		)
			.andExpect(status().isConflict)
			.andExpect(jsonPath("$.status").value(409))
	}

	@Test
	@DisplayName("예약 조회 - 200 OK")
	fun getReservation_200() {
		whenever(reservationService.getReservation(1L)).thenReturn(sampleResponse())

		mockMvc.perform(get("/api/v1/reservations/1"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.roomTypeName").value("그랜드 디럭스"))
	}

	@Test
	@DisplayName("존재하지 않는 예약 조회 - 404")
	fun getReservation_404() {
		whenever(reservationService.getReservation(999L))
			.thenThrow(ReservationNotFoundException(999L))

		mockMvc.perform(get("/api/v1/reservations/999"))
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.status").value(404))
	}

	@Test
	@DisplayName("예약 취소 - 200 OK")
	fun cancelReservation_200() {
		val cancelledResponse = sampleResponse().copy(status = ReservationStatus.CANCELLED)
		whenever(reservationService.cancelReservation(1L)).thenReturn(cancelledResponse)

		mockMvc.perform(patch("/api/v1/reservations/1/cancel"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value("CANCELLED"))
	}

	@Test
	@DisplayName("날짜 범위 오류 - 400 Bad Request")
	fun createReservation_400_invalidDateRange() {
		whenever(reservationService.createReservation(any()))
			.thenThrow(IllegalArgumentException("체크인 날짜는 체크아웃 날짜보다 이전이어야 합니다."))

		val request =
			CreateReservationRequest(
				roomTypeId = 1,
				guestName = "테스트유저1",
				guestEmail = "testuser1@test.com",
				checkInDate = checkOutDate,
				checkOutDate = checkInDate
			)

		mockMvc.perform(
			post("/api/v1/reservations")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.status").value(400))
	}

	@Test
	@DisplayName("이미 취소된 예약 취소 - 409")
	fun cancelReservation_409() {
		whenever(reservationService.cancelReservation(1L))
			.thenThrow(InvalidReservationStateException(1L, ReservationStatus.CANCELLED))

		mockMvc.perform(patch("/api/v1/reservations/1/cancel"))
			.andExpect(status().isConflict)
	}
}
