package mm.nudesprotectorback.user.web.dto

import java.time.Instant

data class UserSettingsResponse(
    val username: String,
    val email: String,
    val createdAt: Instant,
    val mfaEnabled: Boolean,
)
