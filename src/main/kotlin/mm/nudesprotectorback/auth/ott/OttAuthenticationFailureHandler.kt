package mm.nudesprotectorback.auth.ott

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets

@Component
class OttAuthenticationFailureHandler(
    private val objectMapper: ObjectMapper,
) : AuthenticationFailureHandler {

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.characterEncoding = StandardCharsets.UTF_8.name()
        response.contentType = "application/json"
        objectMapper.writeValue(response.writer, mapOf("message" to resolveFailureMessage(exception)))
    }

    private fun resolveFailureMessage(exception: AuthenticationException): String =
        exception.message?.takeIf { it.isNotBlank() } ?: "One-time token is invalid or expired."
}
