package mm.nudesprotectorback.domain.dto.response

data class MfaLoginResponse(
    val otpRequired: Boolean,
    val message: String,
)
