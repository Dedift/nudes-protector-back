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
import org.springframework.security.web.authentication.RememberMeServices
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Service

@Service
class MfaAuthenticationService(
    private val authenticationManager: AuthenticationManager,
    private val rememberMeServices: RememberMeServices,
) {
    companion object {
        private const val REMEMBER_ME_SESSION_ATTRIBUTE = "AUTH_REMEMBER_ME_REQUESTED"
    }

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
            applyRememberMeIfRequested(
                rememberMe = request.rememberMe,
                authentication = authentication,
                httpServletRequest = httpServletRequest,
                httpServletResponse = httpServletResponse,
            )
            return MfaLoginResponse(
                otpRequired = false,
                message = "Authenticated successfully",
            )
        }

        httpServletRequest.session.setAttribute(REMEMBER_ME_SESSION_ATTRIBUTE, request.rememberMe)

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
        applyRememberMeIfRequested(
            rememberMe = httpServletRequest.session.getAttribute(REMEMBER_ME_SESSION_ATTRIBUTE) as? Boolean ?: false,
            authentication = authentication,
            httpServletRequest = httpServletRequest,
            httpServletResponse = httpServletResponse,
        )
        httpServletRequest.session.removeAttribute(REMEMBER_ME_SESSION_ATTRIBUTE)

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

    private fun applyRememberMeIfRequested(
        rememberMe: Boolean,
        authentication: org.springframework.security.core.Authentication,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
    ) {
        if (!rememberMe) {
            return
        }
        rememberMeServices.loginSuccess(httpServletRequest, httpServletResponse, authentication)
    }
}
