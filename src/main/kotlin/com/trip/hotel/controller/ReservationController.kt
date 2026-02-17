package com.trip.hotel.controller

import com.trip.hotel.dto.request.CreateReservationRequest
import com.trip.hotel.dto.response.ErrorResponse
import com.trip.hotel.dto.response.ReservationResponse
import com.trip.hotel.service.ReservationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 예약 API. 예약 생성, 조회, 취소 기능을 제공한다. */
@Tag(name = "예약", description = "예약 생성, 조회, 취소 API")
@RestController
@RequestMapping("/api/v1/reservations")
class ReservationController(
	private val reservationService: ReservationService,
) {
	/** POST /api/v1/reservations — 예약 생성. 성공 시 201 Created. */
	@Operation(summary = "예약 생성", description = "재고 확인 후 예약을 생성한다. FOR UPDATE 잠금으로 동시성을 제어한다.")
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "201", description = "예약 생성 성공"),
			ApiResponse(
				responseCode = "400",
				description = "입력 유효성 검사 실패",
				content = [Content(schema = Schema(implementation = ErrorResponse::class))],
			),
			ApiResponse(
				responseCode = "404",
				description = "존재하지 않는 룸 타입",
				content = [Content(schema = Schema(implementation = ErrorResponse::class))],
			),
			ApiResponse(
				responseCode = "409",
				description = "재고 부족",
				content = [Content(schema = Schema(implementation = ErrorResponse::class))],
			),
		],
	)
	@PostMapping
	fun createReservation(
		@Valid @RequestBody request: CreateReservationRequest,
	): ResponseEntity<ReservationResponse> {
		val response = reservationService.createReservation(request)
		return ResponseEntity.status(HttpStatus.CREATED).body(response)
	}

	/** GET /api/v1/reservations/{id} — 예약 단건 조회. */
	@Operation(summary = "예약 조회", description = "예약 ID로 단건 조회한다.")
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "조회 성공"),
			ApiResponse(
				responseCode = "404",
				description = "존재하지 않는 예약",
				content = [Content(schema = Schema(implementation = ErrorResponse::class))],
			),
		],
	)
	@GetMapping("/{id}")
	fun getReservation(
		@Parameter(description = "예약 ID", example = "1")
		@PathVariable
		id: Long,
	): ResponseEntity<ReservationResponse> {
		val response = reservationService.getReservation(id)
		return ResponseEntity.ok(response)
	}

	/** PATCH /api/v1/reservations/{id}/cancel — 예약 취소. 재고 복원 포함. */
	@Operation(summary = "예약 취소", description = "CONFIRMED 상태의 예약을 취소하고 재고를 복원한다.")
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "취소 성공"),
			ApiResponse(
				responseCode = "404",
				description = "존재하지 않는 예약",
				content = [Content(schema = Schema(implementation = ErrorResponse::class))],
			),
			ApiResponse(
				responseCode = "409",
				description = "이미 취소된 예약",
				content = [Content(schema = Schema(implementation = ErrorResponse::class))],
			),
		],
	)
	@PatchMapping("/{id}/cancel")
	fun cancelReservation(
		@Parameter(description = "예약 ID", example = "1")
		@PathVariable
		id: Long,
	): ResponseEntity<ReservationResponse> {
		val response = reservationService.cancelReservation(id)
		return ResponseEntity.ok(response)
	}
}
