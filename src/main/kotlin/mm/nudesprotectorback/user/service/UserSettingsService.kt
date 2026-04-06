package mm.nudesprotectorback.user.service

import mm.nudesprotectorback.user.repository.UserRepository
import mm.nudesprotectorback.user.web.dto.UserSettingsResponse
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserSettingsService(
    private val userRepository: UserRepository,
) {
    fun getSettings(email: String): UserSettingsResponse {
        val user = loadUser(email)
        return UserSettingsResponse(
            email = user.email,
            mfaEnabled = user.mfaEnabled,
        )
    }

    fun updateMfa(email: String, enabled: Boolean): UserSettingsResponse {
        val user = loadUser(email)
        val updatedUser = userRepository.save(user.copy(mfaEnabled = enabled))
        return UserSettingsResponse(
            email = updatedUser.email,
            mfaEnabled = updatedUser.mfaEnabled,
        )
    }

    private fun loadUser(email: String) =
        userRepository.findByEmailIgnoreCase(email)
            ?: throw UsernameNotFoundException("User with email '$email' was not found")
}
