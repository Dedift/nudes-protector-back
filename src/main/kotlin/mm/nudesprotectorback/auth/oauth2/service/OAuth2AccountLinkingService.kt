package mm.nudesprotectorback.auth.oauth2.service

import mm.nudesprotectorback.auth.oauth2.model.UserOAuth2Account
import mm.nudesprotectorback.auth.oauth2.repository.UserOAuth2AccountRepository
import mm.nudesprotectorback.auth.passkey.service.PasskeyUserEntityService
import mm.nudesprotectorback.user.model.User
import mm.nudesprotectorback.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OAuth2AccountLinkingService(
    private val userRepository: UserRepository,
    private val userOAuth2AccountRepository: UserOAuth2AccountRepository,
    private val passkeyUserEntityService: PasskeyUserEntityService,
    private val passwordEncoder: PasswordEncoder,
) {
    fun loadOrCreateUser(
        provider: String,
        providerUserId: String,
        email: String,
        usernameCandidate: String,
    ): User {
        val normalizedProvider = provider.trim().lowercase()
        val normalizedProviderUserId = providerUserId.trim()
        val normalizedEmail = email.trim().lowercase()

        val linkedAccount = userOAuth2AccountRepository.findByProviderAndProviderUserId(
            normalizedProvider,
            normalizedProviderUserId,
        )
        if (linkedAccount != null) {
            val linkedUser = userRepository.findById(linkedAccount.userId).orElseThrow {
                IllegalStateException("User '${linkedAccount.userId}' linked to OAuth2 account was not found")
            }
            passkeyUserEntityService.ensureExists(linkedUser)
            return linkedUser
        }

        val existingUser = userRepository.findByEmailIgnoreCase(normalizedEmail)
        val user = existingUser ?: createUser(
            email = normalizedEmail,
            usernameCandidate = usernameCandidate,
        )

        if (userOAuth2AccountRepository.findByUserIdAndProvider(checkNotNull(user.id), normalizedProvider) == null) {
            userOAuth2AccountRepository.save(
                UserOAuth2Account(
                    provider = normalizedProvider,
                    providerUserId = normalizedProviderUserId,
                    userId = checkNotNull(user.id),
                )
            )
        }

        passkeyUserEntityService.ensureExists(user)
        return user
    }

    private fun createUser(
        email: String,
        usernameCandidate: String,
    ): User {
        val username = normalizeUsername(usernameCandidate, email)
        val passwordHash = requireNotNull(passwordEncoder.encode(UUID.randomUUID().toString())) {
            "Password encoder returned null hash"
        }

        return userRepository.save(
            User(
                username = username,
                email = email,
                passwordHash = passwordHash,
                emailVerified = true,
            )
        )
    }

    private fun normalizeUsername(
        usernameCandidate: String,
        email: String,
    ): String {
        val fallback = email.substringBefore("@")
        val normalized = usernameCandidate.trim().ifBlank { fallback }
        return normalized.take(50)
    }
}
