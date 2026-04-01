package mm.nudesprotectorback.auth.web.dto

data class MfaVerifyResponse(
    val authenticated: Boolean,
    val message: String,
)
