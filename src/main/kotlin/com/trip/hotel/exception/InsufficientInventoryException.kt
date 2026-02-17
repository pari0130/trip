package com.trip.hotel.exception

import org.springframework.http.HttpStatus
import java.time.LocalDate

/** 재고 부족 시 발생. 요청 수량이 잔여 재고를 초과할 때. HTTP 409. */
class InsufficientInventoryException(
	roomTypeId: Long,
	date: LocalDate,
	requested: Int,
	available: Int
) : BusinessException(
		status = HttpStatus.CONFLICT,
		message = "재고가 부족합니다. roomTypeId=$roomTypeId, 날짜=$date, 요청=$requested, 잔여=$available"
	)
