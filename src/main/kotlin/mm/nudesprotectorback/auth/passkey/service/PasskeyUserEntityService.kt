package mm.nudesprotectorback.auth.passkey.service

import mm.nudesprotectorback.user.model.User
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.stereotype.Service
import java.nio.ByteBuffer
import java.util.UUID

@Service
class PasskeyUserEntityService(
    private val publicKeyCredentialUserEntityRepository: PublicKeyCredentialUserEntityRepository,
) {
    fun ensureExists(user: User) {
        val email = user.email
        if (publicKeyCredentialUserEntityRepository.findByUsername(email) != null) {
            return
        }

        val userEntity = ImmutablePublicKeyCredentialUserEntity.builder()
            .name(email)
            .displayName(user.username)
            .id(toBytes(checkNotNull(user.id)))
            .build()

        publicKeyCredentialUserEntityRepository.save(userEntity)
    }

    private fun toBytes(id: UUID): Bytes {
        val buffer = ByteBuffer.allocate(16)
        buffer.putLong(id.mostSignificantBits)
        buffer.putLong(id.leastSignificantBits)
        return Bytes(buffer.array())
    }
}
