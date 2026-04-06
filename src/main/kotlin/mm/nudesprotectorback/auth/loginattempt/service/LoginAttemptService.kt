package mm.nudesprotectorback.auth.loginattempt.service

import mm.nudesprotectorback.auth.loginattempt.model.LoginAttemptState
import mm.nudesprotectorback.auth.loginattempt.repository.LoginAttemptStateRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

@Service
class LoginAttemptService(
    private val loginAttemptStateRepository: LoginAttemptStateRepository,
    @Value($$"${app.security.login.max-attempts:5}")
    private val maxAttempts: Int,
    @Value($$"${app.security.login.lock-duration:PT15M}")
    private val lockDuration: Duration,
) {
    fun isLocked(userId: UUID): Boolean {
        val state = loginAttemptStateRepository.findById(userId).orElse(null) ?: return false
        return state.attempts >= maxAttempts
    }

    fun registerFailure(userId: UUID) {
        val currentState = loginAttemptStateRepository.findById(userId).orElse(null)
        val nextState = currentState?.increment() ?: LoginAttemptState.initial(userId, lockDuration)
        loginAttemptStateRepository.save(nextState)
    }

    fun clear(userId: UUID) {
        loginAttemptStateRepository.deleteById(userId)
    }
}
