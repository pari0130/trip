package com.trip.hotel.service

import com.trip.hotel.domain.entity.Inventory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * In-Memory 재고 카운터. DB 접근 전 사전 필터링으로 불필요한 잠금 경합을 방지한다.
 * - 카운터는 성능 최적화 레이어이며, DB가 유일한 정합성 원천(source of truth)이다.
 * - key: "roomTypeId:date" (e.g., "1:2026-02-18")
 */
@Service
class InventoryCounterService {
	private val counters = ConcurrentHashMap<String, AtomicInteger>()

	/**
	 * DB 재고 목록으로 카운터를 초기화한다.
	 * 기존 카운터를 모두 제거 후, Inventory.availableQuantity 기준으로 재설정.
	 * ApplicationRunner(InventoryCounterInitializer)에서 앱 시작 시 1회 호출.
	 */
	fun initialize(inventories: List<Inventory>) {
		counters.clear()
		inventories.forEach { inventory ->
			val key = buildKey(inventory.roomType.id, inventory.date)
			counters[key] = AtomicInteger(inventory.availableQuantity)
		}
	}

	/**
	 * 선차감 시도. 날짜를 순차 순회하며 AtomicInteger.addAndGet(-quantity).
	 * 중간에 음수 발생 또는 미등록 날짜 → 해당 날짜 복원 + 이전 날짜 전부 롤백 → false 반환.
	 */
	fun tryDecrement(
		roomTypeId: Long,
		dates: List<LocalDate>,
		quantity: Int
	): Boolean {
		val decremented = mutableListOf<String>()

		for (date in dates) {
			val key = buildKey(roomTypeId, date)
			val counter =
				counters[key] ?: run {
					rollback(decremented, quantity)
					return false
				}

			val newValue = counter.addAndGet(-quantity)
			if (newValue < 0) {
				counter.addAndGet(quantity)
				rollback(decremented, quantity)
				return false
			}
			decremented.add(key)
		}
		return true
	}

	/**
	 * 카운터 복원. 지정된 날짜들의 카운터를 quantity만큼 증가시킨다.
	 * 호출 시점:
	 * - 예약 취소: DB 재고 복원 후 카운터도 동일하게 복원
	 * - DB 실패 보상: tryDecrement 성공 후 DB 트랜잭션 실패 시 catch에서 되돌림
	 * 미등록 키는 무시한다 (null-safe).
	 */
	fun increment(
		roomTypeId: Long,
		dates: List<LocalDate>,
		quantity: Int
	) {
		for (date in dates) {
			val key = buildKey(roomTypeId, date)
			counters[key]?.addAndGet(quantity)
		}
	}

	/** 카운터 현재 값 조회. 미등록 키는 null 반환. 주로 테스트에서 카운터-DB 일치 검증용. */
	fun getCount(
		roomTypeId: Long,
		date: LocalDate
	): Int? {
		return counters[buildKey(roomTypeId, date)]?.get()
	}

	/** 카운터 키 생성. 형식: "roomTypeId:date" (e.g., "1:2026-02-18"). */
	private fun buildKey(
		roomTypeId: Long,
		date: LocalDate
	): String = "$roomTypeId:$date"

	/** tryDecrement 중간 실패 시, 이미 차감된 날짜들의 카운터를 원래 값으로 되돌린다. */
	private fun rollback(
		decrementedKeys: List<String>,
		quantity: Int
	) {
		decrementedKeys.forEach { key ->
			counters[key]?.addAndGet(quantity)
		}
	}
}
