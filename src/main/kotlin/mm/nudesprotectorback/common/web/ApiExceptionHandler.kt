package mm.nudesprotectorback.common.web

import mm.nudesprotectorback.common.web.dto.ApiErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.authentication.DisabledException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(LockedException::class)
    fun handleLocked(exception: LockedException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity
            .status(HttpStatus.LOCKED)
            .body(ApiErrorResponse("Account is locked for 15 minutes after too many failed login attempts."))

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(exception: AuthenticationException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiErrorResponse(mapAuthenticationMessage(exception.message)))

    @ExceptionHandler(DisabledException::class)
    fun handleDisabled(exception: DisabledException): ResponseEntity<ApiErrorResponse> =
        ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiErrorResponse("Email is not verified yet."))

    private fun mapAuthenticationMessage(message: String?): String =
        when (message) {
            "Invalid email or password" -> "Invalid email or password."
            "Invalid OTP" -> "The login code is incorrect. Check the latest email and try again."
            "OTP expired" -> "The login code has expired. Request a new code and try again."
            "Invalid OTP. A new code has been sent" -> "The login code is no longer valid. A new code has been sent to your email."
            else -> message ?: "Authentication failed."
        }
}
