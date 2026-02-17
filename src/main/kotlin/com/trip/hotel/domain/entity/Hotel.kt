package com.trip.hotel.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import java.time.LocalDateTime

/** 호텔 엔티티. 호텔 기본 정보를 관리한다. */
@Entity
class Hotel(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0,
	@Column(nullable = false)
	val name: String,
	@Column(nullable = false, length = 500)
	val address: String,
	@Column(length = 1000)
	val description: String? = null,
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
