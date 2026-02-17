package com.trip.hotel.exception

import org.springframework.http.HttpStatus

/** 존재하지 않는 룸 타입 조회 시 발생. HTTP 404. */
class RoomTypeNotFoundException(roomTypeId: Long) : BusinessException(
	status = HttpStatus.NOT_FOUND,
	message = "룸 타입을 찾을 수 없습니다: roomTypeId=$roomTypeId"
)
