package mm.nudesprotectorback.repository

import mm.nudesprotectorback.domain.EmailVerificationCode
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface EmailVerificationCodeRepository : CrudRepository<EmailVerificationCode, UUID> {
    fun findByUserIdAndCode(userId: UUID, code: String): EmailVerificationCode?
    fun deleteByUserId(userId: UUID): Long
}
