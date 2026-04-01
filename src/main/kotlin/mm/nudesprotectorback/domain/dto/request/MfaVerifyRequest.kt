package mm.nudesprotectorback.domain.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class MfaVerifyRequest(
    @field:NotBlank
    @field:Email
    val email: String,
    @field:NotBlank
    @field:Pattern(regexp = "\\d{6}", message = "OTP must contain 6 digits")
    val code: String,
)
