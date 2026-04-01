package mm.nudesprotectorback.security.provider

import mm.nudesprotectorback.repository.UserRepository
import mm.nudesprotectorback.security.token.EmailPasswordAuthenticationToken
import mm.nudesprotectorback.service.MfaLoginService
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class EmailPasswordAuthenticationProvider(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val mfaLoginService: MfaLoginService,
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication {
        val email = authentication.name.trim().lowercase()
        val rawPassword = authentication.credentials.toString()
        val user = userRepository.findByEmailIgnoreCase(email)
            ?: throw BadCredentialsException("Invalid email or password")

        if (!passwordEncoder.matches(rawPassword, user.passwordHash)) {
            throw BadCredentialsException("Invalid email or password")
        }
        if (!user.emailVerified) {
            throw DisabledException("Email is not verified")
        }

        mfaLoginService.issueOtp(user)
        return EmailPasswordAuthenticationToken(
            email = user.email,
            password = "",
        )
    }

    override fun supports(authentication: Class<*>): Boolean =
        EmailPasswordAuthenticationToken::class.java.isAssignableFrom(authentication)
}
