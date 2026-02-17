package com.trip.hotel.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

/** 예약 생성 요청 DTO. Jakarta Validation으로 입력값을 검증한다. */
data class CreateReservationRequest(
	@field:NotNull(message = "룸 타입 ID는 필수입니다.")
	val roomTypeId: Long,
	@field:NotBlank(message = "투숙객 이름은 필수입니다.")
	@field:Size(max = 255, message = "투숙객 이름은 255자 이내여야 합니다.")
	val guestName: String,
	@field:NotBlank(message = "이메일은 필수입니다.")
	@field:Email(message = "올바른 이메일 형식이 아닙니다.")
	@field:Size(max = 255, message = "이메일은 255자 이내여야 합니다.")
	val guestEmail: String,
	@field:NotNull(message = "체크인 날짜는 필수입니다.")
	@field:FutureOrPresent(message = "체크인 날짜는 오늘 이후여야 합니다.")
	val checkInDate: LocalDate,
	@field:NotNull(message = "체크아웃 날짜는 필수입니다.")
	@field:Future(message = "체크아웃 날짜는 내일 이후여야 합니다.")
	val checkOutDate: LocalDate,
	@field:Min(value = 1, message = "객실 수는 1 이상이어야 합니다.")
	val numberOfRooms: Int = 1
)
