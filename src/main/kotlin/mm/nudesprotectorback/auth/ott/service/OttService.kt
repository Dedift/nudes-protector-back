package mm.nudesprotectorback.auth.ott.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mm.nudesprotectorback.auth.ott.model.OttToken
import mm.nudesprotectorback.auth.ott.repository.OttTokenRepository
import mm.nudesprotectorback.auth.security.DatabaseUserDetailsService
import mm.nudesprotectorback.mail.MailService
import mm.nudesprotectorback.user.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

@Service
class OttService(
    private val ottTokenRepository: OttTokenRepository,
    private val userRepository: UserRepository,
    private val databaseUserDetailsService: DatabaseUserDetailsService,
    private val mailService: MailService,
    @Value($$"${app.frontend.base-url:http://localhost:3000}")
    private val frontendBaseUrl: String,
    @Value($$"${app.security.ott.ttl:PT10M}")
    private val ottTtl: Duration,
) {
    fun issueToken(email: String) {
        val normalizedEmail = email.trim().lowercase()
        val user = userRepository.findByEmailIgnoreCase(normalizedEmail)

        if (user == null || !user.emailVerified) {
            return
        }

        val token = UUID.randomUUID().toString()
        ottTokenRepository.save(OttToken.newToken(token, user.email, ottTtl))

        mailService.sendTextMail(
            email = user.email,
            subject = "Your magic sign-in link",
            text = """
                Open this magic sign-in link:
                ${buildMagicLink(token)}

                The link works only once and expires in ${ottTtl.toMinutes()} minutes.
            """.trimIndent(),
        )
    }

    fun authenticate(
        token: String,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
    ) {
        val normalizedToken = token.trim()
        val storedToken = ottTokenRepository.findById(normalizedToken).orElse(null)
            ?: throw BadCredentialsException("Magic link is invalid or expired.")

        ottTokenRepository.deleteById(normalizedToken)

        val user = userRepository.findByEmailIgnoreCase(storedToken.email)
            ?: throw BadCredentialsException("Magic link is invalid or expired.")

        if (!user.emailVerified) {
            throw DisabledException("Email is not verified")
        }

        val userDetails = databaseUserDetailsService.loadUserByUsername(user.email)
        val authentication = UsernamePasswordAuthenticationToken.authenticated(
            userDetails,
            null,
            userDetails.authorities,
        )

        val securityContext = SecurityContextHolder.createEmptyContext()
        securityContext.authentication = authentication
        SecurityContextHolder.setContext(securityContext)
        HttpSessionSecurityContextRepository().saveContext(
            securityContext,
            httpServletRequest,
            httpServletResponse,
        )
    }

    private fun buildMagicLink(token: String): String =
        "${frontendBaseUrl.trimEnd('/')}/api/login/ott?token=$token"
}
