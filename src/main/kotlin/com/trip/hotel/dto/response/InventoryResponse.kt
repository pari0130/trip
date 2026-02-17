package com.trip.hotel.dto.response

import java.time.LocalDate

/** 재고 조회 응답 DTO. 룸타입 정보와 일별 재고 현황을 포함한다. */
data class InventoryResponse(
	val roomTypeId: Long,
	val roomTypeName: String,
	val checkInDate: LocalDate,
	val checkOutDate: LocalDate,
	val dailyAvailabilities: List<DailyAvailability>
)

/** 일별 재고 상세. 날짜별 전체 수량과 잔여 수량. */
data class DailyAvailability(
	val date: LocalDate,
	val totalQuantity: Int,
	val availableQuantity: Int
)
