package mm.nudesprotectorback.user.web.dto

data class VerifyEmailResponse(
    val verified: Boolean,
    val message: String,
)
