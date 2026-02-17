package com.trip.hotel.domain.repository

import com.trip.hotel.domain.entity.Guest
import org.springframework.data.jpa.repository.JpaRepository

interface GuestRepository : JpaRepository<Guest, Long> {
	fun findByEmail(email: String): Guest?
}
