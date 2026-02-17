package com.trip.hotel.controller

import com.trip.hotel.dto.response.ErrorResponse
import com.trip.hotel.dto.response.InventoryResponse
import com.trip.hotel.service.InventoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/** 재고 조회 API. 룸타입별 일별 잔여 재고 현황을 제공한다. */
@Tag(name = "재고", description = "룸타입별 일별 재고 조회 API")
@RestController
@RequestMapping("/api/v1/inventory")
class InventoryController(
	private val inventoryService: InventoryService,
) {
	/** GET /api/v1/inventory?roomTypeId={id}&checkInDate={date}&checkOutDate={date} */
	@Operation(summary = "재고 조회", description = "룸타입별 체크인~체크아웃 기간의 일별 잔여 재고를 조회한다.")
	@ApiResponses(
		value = [
			ApiResponse(responseCode = "200", description = "조회 성공"),
			ApiResponse(
				responseCode = "400",
				description = "필수 파라미터 누락",
				content = [Content(schema = Schema(implementation = ErrorResponse::class))],
			),
			ApiResponse(
				responseCode = "404",
				description = "존재하지 않는 룸 타입",
				content = [Content(schema = Schema(implementation = ErrorResponse::class))],
			),
		],
	)
	@GetMapping
	fun getAvailability(
		@Parameter(description = "룸 타입 ID", example = "1", required = true)
		@RequestParam
		roomTypeId: Long,
		@Parameter(description = "체크인 날짜 (포함)", example = "2026-03-01", required = true)
		@RequestParam
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
		checkInDate: LocalDate,
		@Parameter(description = "체크아웃 날짜 (미포함)", example = "2026-03-03", required = true)
		@RequestParam
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
		checkOutDate: LocalDate,
	): ResponseEntity<InventoryResponse> {
		val response = inventoryService.getAvailability(roomTypeId, checkInDate, checkOutDate)
		return ResponseEntity.ok(response)
	}
}
