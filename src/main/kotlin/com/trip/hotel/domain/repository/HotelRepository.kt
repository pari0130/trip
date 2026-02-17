package com.trip.hotel.domain.repository

import com.trip.hotel.domain.entity.Hotel
import org.springframework.data.jpa.repository.JpaRepository

interface HotelRepository : JpaRepository<Hotel, Long>
