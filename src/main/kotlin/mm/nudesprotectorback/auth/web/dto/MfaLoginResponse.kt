package mm.nudesprotectorback.auth.web.dto

data class MfaLoginResponse(
    val otpRequired: Boolean,
    val message: String,
)
