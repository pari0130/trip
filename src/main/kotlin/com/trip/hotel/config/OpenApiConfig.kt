package com.trip.hotel.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
	@Bean
	fun openAPI(): OpenAPI =
		OpenAPI()
			.info(
				Info()
					.title("호텔 예약 API")
					.description("호텔 객실 재고 관리 및 예약 서비스 API")
					.version("v1"),
			)
}
