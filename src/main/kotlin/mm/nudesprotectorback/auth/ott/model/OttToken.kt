package mm.nudesprotectorback.auth.ott.model

import org.springframework.data.annotation.Id
import org.springframework.data.redis.core.RedisHash
import org.springframework.data.redis.core.TimeToLive
import java.time.Duration

@RedisHash("auth:ott")
data class OttToken(
    @Id
    val token: String,
    val email: String,
    @TimeToLive
    val ttlSeconds: Long,
) {
    companion object {
        fun newToken(
            token: String,
            email: String,
            ttl: Duration,
        ): OttToken = OttToken(
            token = token,
            email = email,
            ttlSeconds = ttl.seconds,
        )
    }
}
