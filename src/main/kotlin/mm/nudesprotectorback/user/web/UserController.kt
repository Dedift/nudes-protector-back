package mm.nudesprotectorback.user.web

import jakarta.validation.Valid
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mm.nudesprotectorback.auth.service.MfaAuthenticationService
import mm.nudesprotectorback.auth.web.dto.MfaLoginRequest
import mm.nudesprotectorback.auth.web.dto.MfaLoginResponse
import mm.nudesprotectorback.auth.web.dto.MfaVerifyRequest
import mm.nudesprotectorback.auth.web.dto.MfaVerifyResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import mm.nudesprotectorback.user.service.EmailVerificationService
import mm.nudesprotectorback.user.service.UserRegistrationService
import mm.nudesprotectorback.user.service.UserSettingsService
import mm.nudesprotectorback.user.web.dto.CreateUserRequest
import mm.nudesprotectorback.user.web.dto.CreateUserResponse
import mm.nudesprotectorback.user.web.dto.UpdateUserMfaRequest
import mm.nudesprotectorback.user.web.dto.UserSettingsResponse
import mm.nudesprotectorback.user.web.dto.VerifyEmailRequest
import mm.nudesprotectorback.user.web.dto.VerifyEmailResponse

@RestController
@RequestMapping("/users")
class UserController(
    private val userRegistrationService: UserRegistrationService,
    private val emailVerificationService: EmailVerificationService,
    private val mfaAuthenticationService: MfaAuthenticationService,
    private val userSettingsService: UserSettingsService,
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

    @GetMapping("/me/settings")
    @ResponseStatus(HttpStatus.OK)
    fun getMySettings(authentication: Authentication): UserSettingsResponse =
        userSettingsService.getSettings(authentication.name)

    @PutMapping("/me/settings/mfa")
    @ResponseStatus(HttpStatus.OK)
    fun updateMyMfa(
        authentication: Authentication,
        @RequestBody request: UpdateUserMfaRequest,
    ): UserSettingsResponse =
        userSettingsService.updateMfa(
            email = authentication.name,
            enabled = request.enabled,
        )
}
