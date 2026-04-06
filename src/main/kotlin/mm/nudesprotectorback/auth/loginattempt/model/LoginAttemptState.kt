package mm.nudesprotectorback.auth.loginattempt.model

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.TimeToLive
import java.time.Duration
import java.util.UUID

@RedisHash("auth:login-attempt")
data class LoginAttemptState(
    @Id
    val userId: UUID,
    val attempts: Int,
    @TimeToLive
    val ttlSeconds: Long,
) {
    companion object {
        fun initial(
            userId: UUID,
            ttl: Duration,
        ): LoginAttemptState = LoginAttemptState(
            userId = userId,
            attempts = 1,
            ttlSeconds = ttl.seconds,
        )
    }

    fun increment(): LoginAttemptState = copy(attempts = attempts + 1)
}
