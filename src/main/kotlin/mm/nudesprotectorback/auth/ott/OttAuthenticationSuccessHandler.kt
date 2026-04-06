package mm.nudesprotectorback.auth.ott

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets

@Component
class OttAuthenticationSuccessHandler(
    private val objectMapper: ObjectMapper,
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        response.status = HttpServletResponse.SC_OK
        response.characterEncoding = StandardCharsets.UTF_8.name()
        response.contentType = "application/json"
        objectMapper.writeValue(response.writer, mapOf("message" to "One-time token accepted."))
    }
}
