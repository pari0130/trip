package com.trip.hotel.exception

import org.springframework.http.HttpStatus

/** 비즈니스 예외 기반 클래스. 모든 도메인 예외는 이 클래스를 상속하며, HTTP 상태 코드를 포함한다. */
abstract class BusinessException(
	val status: HttpStatus,
	override val message: String
) : RuntimeException(message)
