package com.trip.hotel.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import java.time.LocalDateTime

/** 투숙객 엔티티. 동일 이메일의 고객은 하나의 레코드로 관리한다. */
@Entity
class Guest(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0,
	@Column(nullable = false)
	val name: String,
	@Column(nullable = false, unique = true)
	val email: String,
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
