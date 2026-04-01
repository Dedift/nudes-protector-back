package mm.nudesprotectorback.auth.security

import mm.nudesprotectorback.auth.mfa.service.MfaLoginService
import mm.nudesprotectorback.auth.mfa.service.MfaOtpService
import mm.nudesprotectorback.auth.mfa.service.OtpVerificationResult
import mm.nudesprotectorback.auth.security.token.EmailOtpAuthenticationToken
import mm.nudesprotectorback.user.repository.UserRepository
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component

@Component
class EmailOtpAuthenticationProvider(
    private val userRepository: UserRepository,
    private val mfaOtpService: MfaOtpService,
    private val mfaLoginService: MfaLoginService,
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication {
        val email = authentication.name.trim().lowercase()
        val code = authentication.credentials.toString()
        val user = userRepository.findByEmailIgnoreCase(email)
            ?: throw BadCredentialsException("User with email '$email' was not found")

        if (!user.emailVerified) {
            throw DisabledException("Email is not verified")
        }

        return when (mfaOtpService.verifyOtp(checkNotNull(user.id), code)) {
            OtpVerificationResult.SUCCESS -> UsernamePasswordAuthenticationToken.authenticated(
                user.email,
                null,
                listOf(SimpleGrantedAuthority("ROLE_USER")),
            )
            OtpVerificationResult.EXPIRED -> throw BadCredentialsException("OTP expired")
            OtpVerificationResult.INVALID -> throw BadCredentialsException("Invalid OTP")
            OtpVerificationResult.REISSUE_REQUIRED -> {
                mfaLoginService.issueOtp(user)
                throw BadCredentialsException("Invalid OTP. A new code has been sent")
            }
        }
    }

    override fun supports(authentication: Class<*>): Boolean =
        EmailOtpAuthenticationToken::class.java.isAssignableFrom(authentication)
}
