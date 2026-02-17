package com.trip.hotel.exception

import org.springframework.http.HttpStatus

/** 존재하지 않는 예약 조회 시 발생. HTTP 404. */
class ReservationNotFoundException(reservationId: Long) : BusinessException(
	status = HttpStatus.NOT_FOUND,
	message = "예약을 찾을 수 없습니다: reservationId=$reservationId"
)
