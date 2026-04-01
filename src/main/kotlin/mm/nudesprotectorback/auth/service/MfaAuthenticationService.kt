package mm.nudesprotectorback.auth.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mm.nudesprotectorback.auth.security.token.EmailOtpAuthenticationToken
import mm.nudesprotectorback.auth.security.token.EmailPasswordAuthenticationToken
import mm.nudesprotectorback.auth.web.dto.MfaLoginRequest
import mm.nudesprotectorback.auth.web.dto.MfaLoginResponse
import mm.nudesprotectorback.auth.web.dto.MfaVerifyRequest
import mm.nudesprotectorback.auth.web.dto.MfaVerifyResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Service

@Service
class MfaAuthenticationService(
    private val authenticationManager: AuthenticationManager,
) {
    fun startAuthentication(
        request: MfaLoginRequest,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
    ): MfaLoginResponse {
        val authentication = authenticationManager.authenticate(
            EmailPasswordAuthenticationToken(
                email = request.email,
                password = request.password,
            )
        )

        if (authentication.isAuthenticated) {
            saveAuthentication(authentication, httpServletRequest, httpServletResponse)
            return MfaLoginResponse(
                otpRequired = false,
                message = "Authenticated successfully",
            )
        }

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

        saveAuthentication(authentication, httpServletRequest, httpServletResponse)

        return MfaVerifyResponse(
            authenticated = true,
            message = "Authenticated successfully",
        )
    }

    private fun saveAuthentication(
        authentication: org.springframework.security.core.Authentication,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
    ) {
        val securityContext = SecurityContextHolder.createEmptyContext()
        securityContext.authentication = authentication
        SecurityContextHolder.setContext(securityContext)
        HttpSessionSecurityContextRepository().saveContext(
            securityContext,
            httpServletRequest,
            httpServletResponse,
        )
    }
}
