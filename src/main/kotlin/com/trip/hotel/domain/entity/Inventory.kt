package com.trip.hotel.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import java.time.LocalDate

/**
 * 재고 엔티티. 룸 타입별 + 날짜별 재고를 관리한다.
 * - uniqueConstraints: 동일 룸타입 + 날짜 조합의 중복 방지
 * - @Version: Optimistic Lock으로 동시 수정 시 충돌 감지 (보조 안전장치)
 */
@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["room_type_id", "date"])])
class Inventory(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0,
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_type_id", nullable = false)
	val roomType: RoomType,
	@Column(nullable = false)
	val date: LocalDate,
	@Column(nullable = false)
	val totalQuantity: Int,
	@Column(nullable = false)
	var availableQuantity: Int,
	@Version
	@Column(nullable = false)
	var version: Long = 0
) {
	/** 재고 차감. 요청 수량이 잔여보다 많으면 예외 발생. */
	fun decreaseAvailableQuantity(quantity: Int) {
		if (availableQuantity < quantity) {
			throw IllegalStateException(
				"재고 부족: date=$date, 요청=$quantity, 잔여=$availableQuantity"
			)
		}
		availableQuantity -= quantity
	}

	/** 재고 복원 (예약 취소 시). totalQuantity를 초과하지 않도록 상한 제한. */
	fun increaseAvailableQuantity(quantity: Int) {
		availableQuantity = minOf(availableQuantity + quantity, totalQuantity)
	}
}
