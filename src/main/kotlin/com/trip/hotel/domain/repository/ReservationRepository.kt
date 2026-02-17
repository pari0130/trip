package com.trip.hotel.domain.repository

import com.trip.hotel.domain.entity.Reservation
import org.springframework.data.jpa.repository.JpaRepository

interface ReservationRepository : JpaRepository<Reservation, Long>
