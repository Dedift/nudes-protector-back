package mm.nudesprotectorback.domain.redis

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.TimeToLive
import java.time.Duration
import java.util.UUID

@RedisHash("mfa:otp")
data class MfaOtp(
    @Id
    val userId: UUID,
    val code: String,
    val attempts: Int = 0,
    @TimeToLive
    val ttl: Long,
) {
    companion object {
        fun newCode(
            userId: UUID,
            code: String,
            ttl: Duration,
        ): MfaOtp = MfaOtp(
            userId = userId,
            code = code,
            attempts = 0,
            ttl = ttl.seconds,
        )
    }
}