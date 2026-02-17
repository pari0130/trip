package com.trip.hotel.dto.response

import com.trip.hotel.domain.entity.Reservation
import com.trip.hotel.domain.entity.ReservationStatus
import java.time.LocalDate
import java.time.LocalDateTime

/** 예약 응답 DTO. Reservation 엔티티를 API 응답 형태로 변환한다. */
data class ReservationResponse(
	val reservationId: Long,
	val roomTypeId: Long,
	val roomTypeName: String,
	val guestName: String,
	val guestEmail: String,
	val checkInDate: LocalDate,
	val checkOutDate: LocalDate,
	val numberOfRooms: Int,
	val status: ReservationStatus,
	val createdAt: LocalDateTime,
	val updatedAt: LocalDateTime
) {
	companion object {
		/** 엔티티 → DTO 변환 팩토리 메서드. */
		fun from(reservation: Reservation): ReservationResponse =
			ReservationResponse(
				reservationId = reservation.id,
				roomTypeId = reservation.roomType.id,
				roomTypeName = reservation.roomType.name,
				guestName = reservation.guestName,
				guestEmail = reservation.guestEmail,
				checkInDate = reservation.checkInDate,
				checkOutDate = reservation.checkOutDate,
				numberOfRooms = reservation.numberOfRooms,
				status = reservation.status,
				createdAt = reservation.createdAt,
				updatedAt = reservation.updatedAt
			)
	}
}
