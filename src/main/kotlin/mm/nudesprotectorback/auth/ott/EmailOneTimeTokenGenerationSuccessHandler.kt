package mm.nudesprotectorback.auth.ott

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mm.nudesprotectorback.mail.MailService
import mm.nudesprotectorback.user.repository.UserRepository
import org.springframework.http.MediaType
import org.springframework.security.authentication.ott.OneTimeToken
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler
import org.springframework.stereotype.Component

@Component
class EmailOneTimeTokenGenerationSuccessHandler(
    private val mailService: MailService,
    private val userRepository: UserRepository,
) : OneTimeTokenGenerationSuccessHandler {

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        oneTimeToken: OneTimeToken,
    ) {
        val email = oneTimeToken.username.trim().lowercase()
        val user = userRepository.findByEmailIgnoreCase(email)

        if (user != null && user.emailVerified) {
            val loginPageUrl = buildLoginPageUrl(request)

            mailService.sendTextMail(
                email = user.email,
                subject = "Your one-time login link",
                text = """
                    Use this one-time token to sign in: ${oneTimeToken.tokenValue}

                    You can submit it on: $loginPageUrl
                    Or send POST ${request.contextPath}/login/ott with form field token.

                    Token expires at: ${oneTimeToken.expiresAt}
                """.trimIndent(),
            )
        }

        response.status = HttpServletResponse.SC_ACCEPTED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write("""{"message":"If the account exists and email is verified, the one-time token has been sent"}""")
    }

    private fun buildLoginPageUrl(request: HttpServletRequest): String {
        val portPart = when (request.serverPort) {
            80, 443 -> ""
            else -> ":${request.serverPort}"
        }
        return "${request.scheme}://${request.serverName}$portPart${request.contextPath}/login/ott"
    }
}
