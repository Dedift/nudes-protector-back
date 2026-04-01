package mm.nudesprotectorback.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mm.nudesprotectorback.domain.dto.request.MfaLoginRequest
import mm.nudesprotectorback.domain.dto.request.MfaVerifyRequest
import mm.nudesprotectorback.domain.dto.response.MfaLoginResponse
import mm.nudesprotectorback.domain.dto.response.MfaVerifyResponse
import mm.nudesprotectorback.security.token.EmailOtpAuthenticationToken
import mm.nudesprotectorback.security.token.EmailPasswordAuthenticationToken
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Service

@Service
class MfaAuthenticationService(
    private val authenticationManager: AuthenticationManager,
) {
    fun startAuthentication(request: MfaLoginRequest): MfaLoginResponse {
        authenticationManager.authenticate(
            EmailPasswordAuthenticationToken(
                email = request.email,
                password = request.password,
            )
        )

        return MfaLoginResponse(
            otpRequired = true,
            message = "OTP sent to email",
        )
    }

    fun completeAuthentication(
        request: MfaVerifyRequest,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
    ): MfaVerifyResponse {
        val authentication = authenticationManager.authenticate(
            EmailOtpAuthenticationToken(
                email = request.email,
                code = request.code,
            )
        )

        val securityContext = SecurityContextHolder.createEmptyContext()
        securityContext.authentication = authentication
        SecurityContextHolder.setContext(securityContext)
        HttpSessionSecurityContextRepository().saveContext(
            securityContext,
            httpServletRequest,
            httpServletResponse,
        )

        return MfaVerifyResponse(
            authenticated = true,
            message = "Authenticated successfully",
        )
    }
}
