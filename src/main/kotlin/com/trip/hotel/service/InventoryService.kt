package com.trip.hotel.service

import com.trip.hotel.domain.repository.InventoryRepository
import com.trip.hotel.domain.repository.RoomTypeRepository
import com.trip.hotel.dto.response.DailyAvailability
import com.trip.hotel.dto.response.InventoryResponse
import com.trip.hotel.exception.RoomTypeNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/** 재고 조회 서비스. 잠금 없이 읽기 전용으로 날짜별 잔여 재고를 조회한다. */
@Service
class InventoryService(
	private val inventoryRepository: InventoryRepository,
	private val roomTypeRepository: RoomTypeRepository
) {
	/** 룸타입별 체크인~체크아웃 기간의 일별 재고 현황을 조회한다. */
	@Transactional(readOnly = true)
	fun getAvailability(
		roomTypeId: Long,
		checkInDate: LocalDate,
		checkOutDate: LocalDate
	): InventoryResponse {
		require(checkInDate.isBefore(checkOutDate)) {
			"체크인 날짜($checkInDate)는 체크아웃 날짜($checkOutDate)보다 이전이어야 합니다."
		}

		val roomType =
			roomTypeRepository.findById(roomTypeId)
				.orElseThrow { RoomTypeNotFoundException(roomTypeId) }

		val inventories =
			inventoryRepository.findByRoomTypeIdAndDateRange(
				roomTypeId,
				checkInDate,
				checkOutDate
			)

		val dailyAvailabilities =
			inventories.map { inventory ->
				DailyAvailability(
					date = inventory.date,
					totalQuantity = inventory.totalQuantity,
					availableQuantity = inventory.availableQuantity
				)
			}

		return InventoryResponse(
			roomTypeId = roomType.id,
			roomTypeName = roomType.name,
			checkInDate = checkInDate,
			checkOutDate = checkOutDate,
			dailyAvailabilities = dailyAvailabilities
		)
	}
}
