package mm.nudesprotectorback.domain.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class MfaLoginRequest(
    @field:NotBlank
    @field:Email
    val email: String,
    @field:NotBlank
    val password: String,
)
