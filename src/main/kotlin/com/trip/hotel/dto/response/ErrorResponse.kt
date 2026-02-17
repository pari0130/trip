package com.trip.hotel.dto.response

import java.time.LocalDateTime

/** 에러 응답 DTO. 모든 예외 응답의 공통 형식. */
data class ErrorResponse(
	val timestamp: LocalDateTime = LocalDateTime.now(),
	val status: Int,
	val error: String,
	val message: String
)
