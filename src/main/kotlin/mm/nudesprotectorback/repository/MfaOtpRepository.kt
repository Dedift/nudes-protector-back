package mm.nudesprotectorback.repository

import mm.nudesprotectorback.domain.redis.MfaOtp
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface MfaOtpRepository : CrudRepository<MfaOtp, UUID>
