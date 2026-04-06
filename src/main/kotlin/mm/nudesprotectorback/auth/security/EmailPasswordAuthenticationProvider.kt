package mm.nudesprotectorback.auth.security

import mm.nudesprotectorback.auth.loginattempt.service.LoginAttemptService
import mm.nudesprotectorback.auth.mfa.service.MfaLoginService
import mm.nudesprotectorback.auth.security.token.EmailPasswordAuthenticationToken
import mm.nudesprotectorback.user.repository.UserRepository
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class EmailPasswordAuthenticationProvider(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val mfaLoginService: MfaLoginService,
    private val loginAttemptService: LoginAttemptService,
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication {
        val email = authentication.name.trim().lowercase()
        val rawPassword = authentication.credentials.toString()
        val user = userRepository.findByEmailIgnoreCase(email)
            ?: throw BadCredentialsException("Invalid email or password")

        if (loginAttemptService.isLocked(checkNotNull(user.id))) {
            throw LockedException("Account is locked for 15 minutes after too many failed login attempts")
        }

        if (!passwordEncoder.matches(rawPassword, user.passwordHash)) {
            val lockedNow = loginAttemptService.registerFailure(checkNotNull(user.id))
            if (lockedNow) {
                throw LockedException("Account is locked for 15 minutes after too many failed login attempts")
            }
            throw BadCredentialsException("Invalid email or password")
        }

        loginAttemptService.clear(checkNotNull(user.id))

        if (!user.emailVerified) {
            throw DisabledException("Email is not verified")
        }

        if (!user.mfaEnabled) {
            return UsernamePasswordAuthenticationToken.authenticated(
                user.email,
                null,
                listOf(SimpleGrantedAuthority("ROLE_USER")),
            )
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
