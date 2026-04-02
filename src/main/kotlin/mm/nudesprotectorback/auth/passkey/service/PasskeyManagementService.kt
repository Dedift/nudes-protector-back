package mm.nudesprotectorback.auth.passkey.service

import mm.nudesprotectorback.auth.passkey.web.dto.PasskeyResponse
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.UserCredentialRepository
import org.springframework.stereotype.Service

@Service
class PasskeyManagementService(
    private val publicKeyCredentialUserEntityRepository: PublicKeyCredentialUserEntityRepository,
    private val userCredentialRepository: UserCredentialRepository,
) {
    fun listForUsername(username: String): List<PasskeyResponse> {
        val userEntity = publicKeyCredentialUserEntityRepository.findByUsername(normalize(username))
            ?: return emptyList()

        return userCredentialRepository.findByUserId(userEntity.id)
            .map { credential ->
                PasskeyResponse(
                    id = credential.credentialId.toBase64UrlString(),
                    label = credential.label,
                    createdAt = credential.created,
                    lastUsedAt = credential.lastUsed,
                    transports = credential.transports.map { it.toString() },
                )
            }
    }

    fun deleteForUsername(username: String, credentialId: String) {
        val userEntity = publicKeyCredentialUserEntityRepository.findByUsername(normalize(username))
            ?: return
        val credentialKey = Bytes.fromBase64(credentialId)
        val credential = userCredentialRepository.findByCredentialId(credentialKey) ?: return

        if (credential.userEntityUserId != userEntity.id) {
            return
        }

        userCredentialRepository.delete(credentialKey)
    }

    private fun normalize(username: String): String = username.trim().lowercase()
}
