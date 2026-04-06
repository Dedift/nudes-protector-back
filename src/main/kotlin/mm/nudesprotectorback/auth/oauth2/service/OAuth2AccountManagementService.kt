package mm.nudesprotectorback.auth.oauth2.service

import mm.nudesprotectorback.auth.oauth2.repository.UserOAuth2AccountRepository
import mm.nudesprotectorback.user.repository.UserRepository
import mm.nudesprotectorback.user.web.dto.OAuth2AccountResponse
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class OAuth2AccountManagementService(
    private val userRepository: UserRepository,
    private val userOAuth2AccountRepository: UserOAuth2AccountRepository,
) {
    fun listAccounts(email: String): List<OAuth2AccountResponse> {
        val user = userRepository.findByEmailIgnoreCase(email)
            ?: throw UsernameNotFoundException("User with email '$email' was not found")

        return userOAuth2AccountRepository.findAllByUserId(checkNotNull(user.id))
            .sortedBy { it.createdAt }
            .map { account ->
                OAuth2AccountResponse(
                    provider = account.provider,
                    createdAt = account.createdAt,
                )
            }
    }

    fun unlink(email: String, provider: String) {
        val user = userRepository.findByEmailIgnoreCase(email)
            ?: throw UsernameNotFoundException("User with email '$email' was not found")
        val normalizedProvider = provider.trim().lowercase()
        val linkedAccount = userOAuth2AccountRepository.findByUserIdAndProvider(checkNotNull(user.id), normalizedProvider)
            ?: return

        userOAuth2AccountRepository.delete(linkedAccount)
    }
}
