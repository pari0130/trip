package com.trip.hotel.service

import com.trip.hotel.domain.entity.Hotel
import com.trip.hotel.domain.entity.Inventory
import com.trip.hotel.domain.entity.Reservation
import com.trip.hotel.domain.entity.ReservationStatus
import com.trip.hotel.domain.entity.RoomType
import com.trip.hotel.domain.repository.InventoryRepository
import com.trip.hotel.domain.repository.ReservationRepository
import com.trip.hotel.domain.repository.RoomTypeRepository
import com.trip.hotel.dto.request.CreateReservationRequest
import com.trip.hotel.exception.InsufficientInventoryException
import com.trip.hotel.exception.InvalidReservationStateException
import com.trip.hotel.exception.ReservationNotFoundException
import com.trip.hotel.exception.RoomTypeNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ReservationServiceTest {
	@Mock
	private lateinit var reservationRepository: ReservationRepository

	@Mock
	private lateinit var inventoryRepository: InventoryRepository

	@Mock
	private lateinit var roomTypeRepository: RoomTypeRepository

	@InjectMocks
	private lateinit var reservationService: ReservationService

	private lateinit var hotel: Hotel
	private lateinit var roomType: RoomType
	private val checkInDate: LocalDate = LocalDate.now().plusDays(1)
	private val checkOutDate: LocalDate = LocalDate.now().plusDays(3)

	@BeforeEach
	fun setUp() {
		hotel = Hotel(id = 1, name = "시그니엘 서울", address = "서울시 송파구")
		roomType =
			RoomType(
				id = 1,
				hotel = hotel,
				name = "그랜드 디럭스",
				price = BigDecimal("350000"),
				maxOccupancy = 2
			)
	}

	@Test
	@DisplayName("예약 생성 성공")
	fun createReservation_success() {
		// given
		val request =
			CreateReservationRequest(
				roomTypeId = 1,
				guestName = "테스트유저1",
				guestEmail = "testuser1@test.com",
				checkInDate = checkInDate,
				checkOutDate = checkOutDate,
				numberOfRooms = 1
			)
		val inventories =
			listOf(
				Inventory(id = 1, roomType = roomType, date = checkInDate, totalQuantity = 10, availableQuantity = 10),
				Inventory(id = 2, roomType = roomType, date = checkInDate.plusDays(1), totalQuantity = 10, availableQuantity = 10)
			)
		val savedReservation =
			Reservation(
				id = 1,
				roomType = roomType,
				guestName = "테스트유저1",
				guestEmail = "testuser1@test.com",
				checkInDate = checkInDate,
				checkOutDate = checkOutDate,
				numberOfRooms = 1
			)

		whenever(roomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType))
		whenever(inventoryRepository.findByRoomTypeIdAndDateRangeForUpdate(1L, checkInDate, checkOutDate))
			.thenReturn(inventories)
		whenever(reservationRepository.save(any<Reservation>())).thenReturn(savedReservation)

		// when
		val response = reservationService.createReservation(request)

		// then
		assertNotNull(response)
		assertEquals(1L, response.reservationId)
		assertEquals("테스트유저1", response.guestName)
		assertEquals(ReservationStatus.CONFIRMED, response.status)
	}

	@Test
	@DisplayName("존재하지 않는 룸 타입으로 예약 시 예외")
	fun createReservation_roomTypeNotFound() {
		val request =
			CreateReservationRequest(
				roomTypeId = 999,
				guestName = "테스트유저1",
				guestEmail = "testuser1@test.com",
				checkInDate = checkInDate,
				checkOutDate = checkOutDate
			)
		whenever(roomTypeRepository.findById(999L)).thenReturn(Optional.empty())

		assertThrows<RoomTypeNotFoundException> {
			reservationService.createReservation(request)
		}
	}

	@Test
	@DisplayName("재고 부족 시 예외")
	fun createReservation_insufficientInventory() {
		val request =
			CreateReservationRequest(
				roomTypeId = 1,
				guestName = "테스트유저1",
				guestEmail = "testuser1@test.com",
				checkInDate = checkInDate,
				checkOutDate = checkOutDate,
				numberOfRooms = 5
			)
		val inventories =
			listOf(
				Inventory(id = 1, roomType = roomType, date = checkInDate, totalQuantity = 10, availableQuantity = 3),
				Inventory(id = 2, roomType = roomType, date = checkInDate.plusDays(1), totalQuantity = 10, availableQuantity = 3)
			)

		whenever(roomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType))
		whenever(inventoryRepository.findByRoomTypeIdAndDateRangeForUpdate(1L, checkInDate, checkOutDate))
			.thenReturn(inventories)

		assertThrows<InsufficientInventoryException> {
			reservationService.createReservation(request)
		}
	}

	@Test
	@DisplayName("예약 조회 성공")
	fun getReservation_success() {
		val reservation =
			Reservation(
				id = 1,
				roomType = roomType,
				guestName = "테스트유저1",
				guestEmail = "testuser1@test.com",
				checkInDate = checkInDate,
				checkOutDate = checkOutDate,
				numberOfRooms = 1
			)
		whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation))

		val response = reservationService.getReservation(1L)

		assertEquals(1L, response.reservationId)
		assertEquals("그랜드 디럭스", response.roomTypeName)
	}

	@Test
	@DisplayName("존재하지 않는 예약 조회 시 예외")
	fun getReservation_notFound() {
		whenever(reservationRepository.findById(999L)).thenReturn(Optional.empty())

		assertThrows<ReservationNotFoundException> {
			reservationService.getReservation(999L)
		}
	}

	@Test
	@DisplayName("예약 취소 성공")
	fun cancelReservation_success() {
		val reservation =
			Reservation(
				id = 1,
				roomType = roomType,
				guestName = "테스트유저1",
				guestEmail = "testuser1@test.com",
				checkInDate = checkInDate,
				checkOutDate = checkOutDate,
				numberOfRooms = 1
			)
		val inventories =
			listOf(
				Inventory(id = 1, roomType = roomType, date = checkInDate, totalQuantity = 10, availableQuantity = 9),
				Inventory(id = 2, roomType = roomType, date = checkInDate.plusDays(1), totalQuantity = 10, availableQuantity = 9)
			)

		whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation))
		whenever(inventoryRepository.findByRoomTypeIdAndDateRangeForUpdate(1L, checkInDate, checkOutDate))
			.thenReturn(inventories)

		val response = reservationService.cancelReservation(1L)

		assertEquals(ReservationStatus.CANCELLED, response.status)
		assertEquals(10, inventories[0].availableQuantity)
		assertEquals(10, inventories[1].availableQuantity)
	}

	@Test
	@DisplayName("체크인 날짜가 체크아웃 날짜와 같으면 예외")
	fun createReservation_sameDate() {
		val sameDate = LocalDate.now().plusDays(1)
		val request =
			CreateReservationRequest(
				roomTypeId = 1,
				guestName = "테스트유저1",
				guestEmail = "testuser1@test.com",
				checkInDate = sameDate,
				checkOutDate = sameDate
			)

		assertThrows<IllegalArgumentException> {
			reservationService.createReservation(request)
		}
	}

	@Test
	@DisplayName("체크인 날짜가 체크아웃 날짜보다 이후이면 예외")
	fun createReservation_checkInAfterCheckOut() {
		val request =
			CreateReservationRequest(
				roomTypeId = 1,
				guestName = "테스트유저1",
				guestEmail = "testuser1@test.com",
				checkInDate = checkOutDate,
				checkOutDate = checkInDate
			)

		assertThrows<IllegalArgumentException> {
			reservationService.createReservation(request)
		}
	}

	@Test
	@DisplayName("이미 취소된 예약 취소 시 예외")
	fun cancelReservation_alreadyCancelled() {
		val reservation =
			Reservation(
				id = 1,
				roomType = roomType,
				guestName = "테스트유저1",
				guestEmail = "testuser1@test.com",
				checkInDate = checkInDate,
				checkOutDate = checkOutDate,
				numberOfRooms = 1,
				status = ReservationStatus.CANCELLED
			)
		whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation))

		assertThrows<InvalidReservationStateException> {
			reservationService.cancelReservation(1L)
		}
	}
}
