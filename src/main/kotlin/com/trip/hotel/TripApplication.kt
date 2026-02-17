package com.trip.hotel

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TripApplication

fun main(args: Array<String>) {
	runApplication<TripApplication>(*args)
}
