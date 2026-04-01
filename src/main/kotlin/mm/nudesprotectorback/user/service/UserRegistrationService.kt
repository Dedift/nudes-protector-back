package mm.nudesprotectorback.user.service

import mm.nudesprotectorback.auth.passkey.service.PasskeyUserEntityService
import mm.nudesprotectorback.user.model.User
import mm.nudesprotectorback.user.repository.UserRepository
import mm.nudesprotectorback.user.web.dto.CreateUserRequest
import mm.nudesprotectorback.user.web.dto.CreateUserResponse
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserRegistrationService(
    private val userRepository: UserRepository,
    private val emailVerificationService: EmailVerificationService,
    private val passkeyUserEntityService: PasskeyUserEntityService,
    private val passwordEncoder: PasswordEncoder,
) {
    fun createUser(request: CreateUserRequest): CreateUserResponse {
        val normalizedEmail = requireNotNull(request.email).trim().lowercase()
        val normalizedUsername = requireNotNull(request.username).trim()
        val rawPassword = requireNotNull(request.password)
        val encodedPassword = requireNotNull(passwordEncoder.encode(rawPassword)) {
            "Password encoder returned null hash"
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw IllegalArgumentException("User with email '$normalizedEmail' already exists")
        }

        val savedUser = userRepository.save(
            User(
                username = normalizedUsername,
                email = normalizedEmail,
                passwordHash = encodedPassword,
                mfaEnabled = request.mfaEnabled,
            )
        )

        passkeyUserEntityService.ensureExists(savedUser)
        emailVerificationService.issueCodeForUser(savedUser)

        return CreateUserResponse(
            id = checkNotNull(savedUser.id),
            username = savedUser.username,
            email = savedUser.email,
            emailVerified = savedUser.emailVerified,
            createdAt = savedUser.createdAt,
        )
    }
}
