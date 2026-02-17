package com.trip.hotel.domain.repository

import com.trip.hotel.domain.entity.Reservation
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface ReservationRepository : JpaRepository<Reservation, Long> {
	@EntityGraph(attributePaths = ["roomType", "guest"])
	override fun findById(id: Long): Optional<Reservation>
}
