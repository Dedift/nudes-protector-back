package mm.nudesprotectorback.user.web.dto

import java.time.Instant

data class OAuth2AccountResponse(
    val provider: String,
    val createdAt: Instant,
)
