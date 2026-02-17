package com.trip.hotel.config

import com.trip.hotel.domain.repository.InventoryRepository
import com.trip.hotel.service.InventoryCounterService
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * 앱 시작 시 DB 재고를 읽어 In-Memory 카운터를 초기화한다.
 * ApplicationRunner: Flyway 마이그레이션 완료 후 실행 보장.
 */
@Component
class InventoryCounterInitializer(
	private val inventoryRepository: InventoryRepository,
	private val inventoryCounterService: InventoryCounterService
) : ApplicationRunner {
	/**
	 * redis 의 레이스 컨디션 local 구현
	 * local 환경 DB 접근 최소화를 위해, 앱 시작 시 DB에서 재고 데이터를 읽어 In-Memory 카운터를 초기화한다.
	 * */
	override fun run(args: ApplicationArguments?) {
		val inventories = inventoryRepository.findAll()
		inventoryCounterService.initialize(inventories)
	}
}
