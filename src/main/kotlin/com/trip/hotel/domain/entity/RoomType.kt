package com.trip.hotel.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PreUpdate
import java.math.BigDecimal
import java.time.LocalDateTime

/** 룸 타입 엔티티. 호텔 내 객실 유형(디럭스, 스위트 등)과 가격 정보를 관리한다. */
@Entity
class RoomType(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0,
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "hotel_id", nullable = false)
	val hotel: Hotel,
	@Column(nullable = false)
	val name: String,
	@Column(length = 1000)
	val description: String? = null,
	@Column(nullable = false, precision = 12, scale = 2)
	val price: BigDecimal,
	@Column(nullable = false)
	val maxOccupancy: Int,
	@Column(nullable = false, updatable = false)
	val createdAt: LocalDateTime = LocalDateTime.now(),
	@Column(nullable = false)
	var updatedAt: LocalDateTime = LocalDateTime.now()
) {
	@PreUpdate
	fun onPreUpdate() {
		updatedAt = LocalDateTime.now()
	}
}
