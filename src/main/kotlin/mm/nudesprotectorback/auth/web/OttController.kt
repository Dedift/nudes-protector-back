package mm.nudesprotectorback.auth.web

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mm.nudesprotectorback.auth.ott.service.OttService
import mm.nudesprotectorback.common.web.dto.ApiErrorResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class OttController(
    private val ottService: OttService,
    @Value($$"${app.frontend.base-url:http://localhost:3000}")
    private val frontendBaseUrl: String,
) {
    @PostMapping("/ott/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun generate(@RequestParam username: String): ApiErrorResponse {
        ottService.issueToken(username)
        return ApiErrorResponse("If the account exists and email is verified, the magic link has been sent.")
    }

    @GetMapping("/api/login/ott")
    fun loginByMagicLink(
        @RequestParam token: String,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
    ) {
        try {
            ottService.authenticate(token, httpServletRequest, httpServletResponse)
            httpServletResponse.sendRedirect(frontendBaseUrl)
        } catch (_: AuthenticationException) {
            httpServletResponse.sendRedirect("${frontendBaseUrl.trimEnd('/')}?screen=login&error=ott_invalid")
        }
    }
}
