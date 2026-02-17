package com.trip.hotel.domain.repository

import com.trip.hotel.domain.entity.Inventory
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface InventoryRepository : JpaRepository<Inventory, Long> {
	/**
	 * 재고 조회용 (잠금 없음).
	 * 메서드 네이밍 쿼리(findByRoomTypeIdAndDateGreaterThanEqualAndDateLessThanOrderByDateAsc)로
	 * 대체 가능하나, 가독성을 위해 @Query 사용.
	 */
	@Query(
		"SELECT i FROM Inventory i WHERE i.roomType.id = :roomTypeId" +
			" AND i.date >= :startDate AND i.date < :endDate ORDER BY i.date ASC",
	)
	fun findByRoomTypeIdAndDateRange(
		@Param("roomTypeId") roomTypeId: Long,
		@Param("startDate") startDate: LocalDate,
		@Param("endDate") endDate: LocalDate,
	): List<Inventory>

	/**
	 * 재고 변경용 (Pessimistic Lock).
	 * SELECT ... FOR UPDATE로 해당 날짜 범위의 재고 행을 잠금.
	 * ORDER BY date ASC: 모든 트랜잭션이 동일 순서로 잠금을 획득하여 데드락 방지.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query(
		"SELECT i FROM Inventory i WHERE i.roomType.id = :roomTypeId" +
			" AND i.date >= :startDate AND i.date < :endDate ORDER BY i.date ASC",
	)
	fun findByRoomTypeIdAndDateRangeForUpdate(
		@Param("roomTypeId") roomTypeId: Long,
		@Param("startDate") startDate: LocalDate,
		@Param("endDate") endDate: LocalDate,
	): List<Inventory>
}
