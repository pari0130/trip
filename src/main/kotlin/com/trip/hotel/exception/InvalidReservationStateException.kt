package com.trip.hotel.exception

import com.trip.hotel.domain.entity.ReservationStatus
import org.springframework.http.HttpStatus

/** 예약 상태가 요청 동작에 부적합할 때 발생 (예: 이미 취소된 예약 재취소). HTTP 409. */
class InvalidReservationStateException(
	reservationId: Long,
	currentStatus: ReservationStatus
) : BusinessException(
		status = HttpStatus.CONFLICT,
		message = "예약 상태가 올바르지 않습니다: reservationId=$reservationId, 현재 상태=$currentStatus"
	)
