package com.trip.hotel.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PreUpdate
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 예약 엔티티. 예약 생명주기: CONFIRMED → CANCELLED (단방향).
 * 취소는 soft delete 방식으로 status만 변경한다.
 */
@Entity
class Reservation(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0,
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_type_id", nullable = false)
	val roomType: RoomType,
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "guest_id", nullable = false)
	val guest: Guest,
	@Column(nullable = false)
	val checkInDate: LocalDate,
	@Column(nullable = false)
	val checkOutDate: LocalDate,
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	var status: ReservationStatus = ReservationStatus.CONFIRMED,
	@Column(nullable = false)
	val numberOfRooms: Int,
	@Column(nullable = false, updatable = false)
	val createdAt: LocalDateTime = LocalDateTime.now(),
	@Column(nullable = false)
	var updatedAt: LocalDateTime = LocalDateTime.now()
) {
	@PreUpdate
	fun onPreUpdate() {
		updatedAt = LocalDateTime.now()
	}

	/** 예약 취소. 상태를 CANCELLED로 변경한다. updatedAt은 @PreUpdate로 자동 갱신. */
	fun cancel() {
		status = ReservationStatus.CANCELLED
	}
}
