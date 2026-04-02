package mm.nudesprotectorback.auth.passkey.web

import mm.nudesprotectorback.auth.passkey.service.PasskeyManagementService
import mm.nudesprotectorback.auth.passkey.web.dto.PasskeyResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users/me/passkeys")
class PasskeyController(
    private val passkeyManagementService: PasskeyManagementService,
) {
    @GetMapping
    fun list(authentication: Authentication): List<PasskeyResponse> =
        passkeyManagementService.listForUsername(authentication.name)

    @DeleteMapping("/{credentialId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        authentication: Authentication,
        @PathVariable credentialId: String,
    ) {
        passkeyManagementService.deleteForUsername(authentication.name, credentialId)
    }
}
