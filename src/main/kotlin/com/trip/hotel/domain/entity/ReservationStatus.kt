package com.trip.hotel.domain.entity

/** 예약 상태. CONFIRMED(확정) → CANCELLED(취소) 단방향 전이만 허용. */
enum class ReservationStatus {
	CONFIRMED,
	CANCELLED,
}
