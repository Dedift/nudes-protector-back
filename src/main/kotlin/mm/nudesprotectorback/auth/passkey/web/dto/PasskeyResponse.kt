package mm.nudesprotectorback.auth.passkey.web.dto

import java.time.Instant

data class PasskeyResponse(
    val id: String,
    val label: String,
    val createdAt: Instant,
    val lastUsedAt: Instant,
    val transports: List<String>,
)
