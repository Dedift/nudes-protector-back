package mm.nudesprotectorback.domain.dto.response

data class MfaVerifyResponse(
    val authenticated: Boolean,
    val message: String,
)
