package com.trip.hotel.exception

import com.trip.hotel.dto.response.ErrorResponse
import jakarta.persistence.PessimisticLockException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/** 전역 예외 처리. 비즈니스 예외, 유효성 검증, 동시성 충돌 등을 통합 처리한다. */
@RestControllerAdvice
class GlobalExceptionHandler {
	private val log = LoggerFactory.getLogger(javaClass)

	/** 비즈니스 예외 → 예외 클래스에 정의된 HTTP 상태 코드로 응답. */
	@ExceptionHandler(BusinessException::class)
	fun handleBusinessException(e: BusinessException): ResponseEntity<ErrorResponse> {
		val response =
			ErrorResponse(
				status = e.status.value(),
				error = e.status.reasonPhrase,
				message = e.message
			)
		return ResponseEntity.status(e.status).body(response)
	}

	/** Jakarta Validation 실패 → 400 Bad Request. 필드별 오류 메시지를 결합하여 반환. */
	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
		val message =
			e.bindingResult.fieldErrors
				.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
		val response =
			ErrorResponse(
				status = HttpStatus.BAD_REQUEST.value(),
				error = "Bad Request",
				message = message
			)
		return ResponseEntity.badRequest().body(response)
	}

	/** 필수 쿼리 파라미터 누락 → 400 Bad Request. */
	@ExceptionHandler(MissingServletRequestParameterException::class)
	fun handleMissingParameter(e: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> {
		val response =
			ErrorResponse(
				status = HttpStatus.BAD_REQUEST.value(),
				error = "Bad Request",
				message = "필수 파라미터가 누락되었습니다: ${e.parameterName}"
			)
		return ResponseEntity.badRequest().body(response)
	}

	/** Optimistic Lock 충돌 (@Version 불일치) → 409 Conflict. 재시도 유도. */
	@ExceptionHandler(ObjectOptimisticLockingFailureException::class)
	fun handleOptimisticLock(e: ObjectOptimisticLockingFailureException): ResponseEntity<ErrorResponse> {
		val response =
			ErrorResponse(
				status = HttpStatus.CONFLICT.value(),
				error = "Conflict",
				message = "동시 수정 충돌이 발생했습니다. 다시 시도해주세요."
			)
		return ResponseEntity.status(HttpStatus.CONFLICT).body(response)
	}

	/** Pessimistic Lock 타임아웃 (잠금 대기 시간 초과) → 503 Service Unavailable. */
	@ExceptionHandler(PessimisticLockException::class)
	fun handlePessimisticLock(e: PessimisticLockException): ResponseEntity<ErrorResponse> {
		val response =
			ErrorResponse(
				status = HttpStatus.SERVICE_UNAVAILABLE.value(),
				error = "Service Unavailable",
				message = "서버가 일시적으로 요청을 처리할 수 없습니다. 잠시 후 다시 시도해주세요."
			)
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response)
	}

	/** 입력값 검증 실패 (require, 날짜 범위 등) → 400 Bad Request. */
	@ExceptionHandler(IllegalArgumentException::class)
	fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
		val response =
			ErrorResponse(
				status = HttpStatus.BAD_REQUEST.value(),
				error = "Bad Request",
				message = e.message ?: "잘못된 요청입니다."
			)
		return ResponseEntity.badRequest().body(response)
	}

	/** 파라미터 타입 변환 실패 (잘못된 날짜 형식 등) → 400 Bad Request. */
	@ExceptionHandler(MethodArgumentTypeMismatchException::class)
	fun handleTypeMismatch(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
		val response =
			ErrorResponse(
				status = HttpStatus.BAD_REQUEST.value(),
				error = "Bad Request",
				message = "파라미터 '${e.name}'의 값이 올바르지 않습니다."
			)
		return ResponseEntity.badRequest().body(response)
	}

	/** 예상치 못한 예외 → 500 Internal Server Error. 스택 트레이스 노출을 방지한다. */
	@ExceptionHandler(Exception::class)
	fun handleUnexpectedException(e: Exception): ResponseEntity<ErrorResponse> {
		log.error("예상치 못한 서버 오류 발생", e)
		val response =
			ErrorResponse(
				status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
				error = "Internal Server Error",
				message = "서버 내부 오류가 발생했습니다."
			)
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
	}
}
