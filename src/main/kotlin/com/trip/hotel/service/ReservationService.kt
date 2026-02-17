package com.trip.hotel.service

import com.trip.hotel.domain.entity.Inventory
import com.trip.hotel.domain.entity.Reservation
import com.trip.hotel.domain.entity.ReservationStatus
import com.trip.hotel.domain.repository.InventoryRepository
import com.trip.hotel.domain.repository.ReservationRepository
import com.trip.hotel.domain.repository.RoomTypeRepository
import com.trip.hotel.dto.request.CreateReservationRequest
import com.trip.hotel.dto.response.ReservationResponse
import com.trip.hotel.exception.InsufficientInventoryException
import com.trip.hotel.exception.InvalidReservationStateException
import com.trip.hotel.exception.ReservationNotFoundException
import com.trip.hotel.exception.RoomTypeNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 예약 서비스. 예약 생성/조회/취소를 처리한다.
 * 동시성 제어: In-Memory 카운터(사전 필터) + Pessimistic Lock(주) + Optimistic Lock(보조) 3중 전략.
 */
@Service
class ReservationService(
	private val reservationRepository: ReservationRepository,
	private val inventoryRepository: InventoryRepository,
	private val roomTypeRepository: RoomTypeRepository,
	private val inventoryCounterService: InventoryCounterService
) {
	/**
	 * 예약 생성.
	 * 1) 룸타입 존재 확인
	 * 2) In-Memory 카운터 선차감 (재고 소진 시 DB 접근 없이 즉시 거절)
	 * 3) SELECT ... FOR UPDATE로 재고 행 잠금
	 * 4) 전 날짜 재고 충분 여부 검증
	 * 5) 재고 차감 후 예약 저장
	 * 6) DB 실패 시 카운터 보상
	 */
	@Transactional
	fun createReservation(request: CreateReservationRequest): ReservationResponse {
		require(request.checkInDate.isBefore(request.checkOutDate)) {
			"체크인 날짜(${request.checkInDate})는 체크아웃 날짜(${request.checkOutDate})보다 이전이어야 합니다."
		}

		val roomType =
			roomTypeRepository.findById(request.roomTypeId)
				.orElseThrow { RoomTypeNotFoundException(request.roomTypeId) }

		val dates = generateDateRange(request.checkInDate, request.checkOutDate)

		// redis 의 레이스 컨디션 local 구현, cache 차감
		if (!inventoryCounterService.tryDecrement(request.roomTypeId, dates, request.numberOfRooms)) {
			throw InsufficientInventoryException(
				roomTypeId = request.roomTypeId,
				date = request.checkInDate,
				requested = request.numberOfRooms,
				available = 0
			)
		}

		try {
			val inventories =
				inventoryRepository.findByRoomTypeIdAndDateRangeForUpdate(
					request.roomTypeId,
					request.checkInDate,
					request.checkOutDate
				)

			validateInventory(inventories, request)
			inventories.forEach { it.decreaseAvailableQuantity(request.numberOfRooms) }

			val reservation =
				reservationRepository.save(
					Reservation(
						roomType = roomType,
						guestName = request.guestName,
						guestEmail = request.guestEmail,
						checkInDate = request.checkInDate,
						checkOutDate = request.checkOutDate,
						numberOfRooms = request.numberOfRooms
					)
				)

			return ReservationResponse.from(reservation)
		} catch (e: Exception) {
			// 예외 발생 시 In-Memory 카운터 복원
			inventoryCounterService.increment(request.roomTypeId, dates, request.numberOfRooms)
			throw e
		}
	}

	/** 예약 단건 조회. */
	@Transactional(readOnly = true)
	fun getReservation(reservationId: Long): ReservationResponse {
		val reservation =
			reservationRepository.findById(reservationId)
				.orElseThrow { ReservationNotFoundException(reservationId) }

		return ReservationResponse.from(reservation)
	}

	/**
	 * 예약 취소.
	 * 1) CONFIRMED 상태인지 확인 (이미 취소된 예약은 재취소 불가)
	 * 2) FOR UPDATE로 재고 행 잠금
	 * 3) 재고 복원 (totalQuantity 초과 방지)
	 * 4) 상태를 CANCELLED로 변경
	 * 5) In-Memory 카운터 복원
	 */
	@Transactional
	fun cancelReservation(reservationId: Long): ReservationResponse {
		val reservation =
			reservationRepository.findById(reservationId)
				.orElseThrow { ReservationNotFoundException(reservationId) }

		if (reservation.status != ReservationStatus.CONFIRMED) {
			throw InvalidReservationStateException(reservationId, reservation.status)
		}

		val inventories =
			inventoryRepository.findByRoomTypeIdAndDateRangeForUpdate(
				reservation.roomType.id,
				reservation.checkInDate,
				reservation.checkOutDate
			)
		inventories.forEach { it.increaseAvailableQuantity(reservation.numberOfRooms) }

		reservation.cancel()

		val dates = generateDateRange(reservation.checkInDate, reservation.checkOutDate)
		inventoryCounterService.increment(reservation.roomType.id, dates, reservation.numberOfRooms)

		return ReservationResponse.from(reservation)
	}

	/** 재고 검증. 기간 내 모든 날짜에 요청 수량 이상의 잔여 재고가 있는지 확인. */
	private fun validateInventory(
		inventories: List<Inventory>,
		request: CreateReservationRequest
	) {
		val expectedDays = ChronoUnit.DAYS.between(request.checkInDate, request.checkOutDate).toInt()
		if (inventories.size < expectedDays) {
			throw InsufficientInventoryException(
				roomTypeId = request.roomTypeId,
				date = request.checkInDate,
				requested = request.numberOfRooms,
				available = 0
			)
		}

		inventories.forEach { inventory ->
			if (inventory.availableQuantity < request.numberOfRooms) {
				throw InsufficientInventoryException(
					roomTypeId = request.roomTypeId,
					date = inventory.date,
					requested = request.numberOfRooms,
					available = inventory.availableQuantity
				)
			}
		}
	}

	private fun generateDateRange(
		checkIn: LocalDate,
		checkOut: LocalDate
	): List<LocalDate> = checkIn.datesUntil(checkOut).toList()
}
