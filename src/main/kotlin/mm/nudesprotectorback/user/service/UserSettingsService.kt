package mm.nudesprotectorback.user.service

import mm.nudesprotectorback.user.web.dto.UpdatePasswordRequest
import mm.nudesprotectorback.user.repository.UserRepository
import mm.nudesprotectorback.user.web.dto.UserSettingsResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserSettingsService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val persistentTokenRepository: PersistentTokenRepository,
) {
    fun getSettings(email: String): UserSettingsResponse {
        val user = loadUser(email)
        return UserSettingsResponse(
            username = user.username,
            email = user.email,
            createdAt = user.createdAt,
            mfaEnabled = user.mfaEnabled,
        )
    }

    fun updateMfa(email: String, enabled: Boolean): UserSettingsResponse {
        val user = loadUser(email)
        val updatedUser = userRepository.save(user.copy(mfaEnabled = enabled))
        return UserSettingsResponse(
            username = updatedUser.username,
            email = updatedUser.email,
            createdAt = updatedUser.createdAt,
            mfaEnabled = updatedUser.mfaEnabled,
        )
    }

    fun updatePassword(email: String, request: UpdatePasswordRequest) {
        val user = loadUser(email)

        if (!passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
            throw BadCredentialsException("Current password is incorrect")
        }

        val newPasswordHash = requireNotNull(passwordEncoder.encode(request.newPassword)) {
            "Password encoder returned null hash"
        }
        userRepository.save(user.copy(passwordHash = newPasswordHash))
        persistentTokenRepository.removeUserTokens(user.email)
    }

    private fun loadUser(email: String) =
        userRepository.findByEmailIgnoreCase(email)
            ?: throw UsernameNotFoundException("User with email '$email' was not found")
}
