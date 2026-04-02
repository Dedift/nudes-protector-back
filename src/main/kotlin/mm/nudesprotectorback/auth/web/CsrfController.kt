package mm.nudesprotectorback.auth.web

import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CsrfController {
    @GetMapping("/csrf")
    fun csrf(csrfToken: CsrfToken): CsrfResponse = CsrfResponse(
        headerName = csrfToken.headerName,
        parameterName = csrfToken.parameterName,
        token = csrfToken.token,
    )
}

data class CsrfResponse(
    val headerName: String,
    val parameterName: String,
    val token: String,
)
