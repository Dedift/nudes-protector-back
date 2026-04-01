package mm.nudesprotectorback.controller

import jakarta.validation.Valid
import mm.nudesprotectorback.domain.dto.request.CreateUserRequest
import mm.nudesprotectorback.domain.dto.request.MfaLoginRequest
import mm.nudesprotectorback.domain.dto.request.MfaVerifyRequest
import mm.nudesprotectorback.domain.dto.request.VerifyEmailRequest
import mm.nudesprotectorback.domain.dto.response.CreateUserResponse
import mm.nudesprotectorback.domain.dto.response.MfaLoginResponse
import mm.nudesprotectorback.domain.dto.response.MfaVerifyResponse
import mm.nudesprotectorback.domain.dto.response.VerifyEmailResponse
import mm.nudesprotectorback.service.EmailVerificationService
import mm.nudesprotectorback.service.MfaAuthenticationService
import mm.nudesprotectorback.service.UserRegistrationService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val userRegistrationService: UserRegistrationService,
    private val emailVerificationService: EmailVerificationService,
    private val mfaAuthenticationService: MfaAuthenticationService,
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(@Valid @RequestBody request: CreateUserRequest): CreateUserResponse =
        userRegistrationService.createUser(request)

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.OK)
    fun verifyEmail(@Valid @RequestBody request: VerifyEmailRequest): VerifyEmailResponse =
        emailVerificationService.verifyEmail(request)

    @PostMapping("/mfa/login")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun mfaLogin(
        @Valid @RequestBody request: MfaLoginRequest,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
    ): MfaLoginResponse =
        mfaAuthenticationService.startAuthentication(
            request = request,
            httpServletRequest = httpServletRequest,
            httpServletResponse = httpServletResponse,
        )

    @PostMapping("/mfa/verify")
    @ResponseStatus(HttpStatus.OK)
    fun mfaVerify(
        @Valid @RequestBody request: MfaVerifyRequest,
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
    ): MfaVerifyResponse =
        mfaAuthenticationService.completeAuthentication(
            request = request,
            httpServletRequest = httpServletRequest,
            httpServletResponse = httpServletResponse,
        )
}
