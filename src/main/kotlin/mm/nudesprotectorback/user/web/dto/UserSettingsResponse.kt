package mm.nudesprotectorback.user.web.dto

data class UserSettingsResponse(
    val email: String,
    val mfaEnabled: Boolean,
)
