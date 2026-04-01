package mm.nudesprotectorback.controller

import jakarta.validation.Valid
import mm.nudesprotectorback.domain.dto.request.CreateUserRequest
import mm.nudesprotectorback.domain.dto.request.VerifyEmailRequest
import mm.nudesprotectorback.domain.dto.response.CreateUserResponse
import mm.nudesprotectorback.domain.dto.response.VerifyEmailResponse
import mm.nudesprotectorback.service.EmailVerificationService
import mm.nudesprotectorback.service.UserRegistrationService
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
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(@Valid @RequestBody request: CreateUserRequest): CreateUserResponse =
        userRegistrationService.createUser(request)

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.OK)
    fun verifyEmail(@Valid @RequestBody request: VerifyEmailRequest): VerifyEmailResponse =
        emailVerificationService.verifyEmail(request)
}
