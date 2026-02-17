package com.trip.hotel.domain.repository

import com.trip.hotel.domain.entity.RoomType
import org.springframework.data.jpa.repository.JpaRepository

interface RoomTypeRepository : JpaRepository<RoomType, Long>
