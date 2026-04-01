package mm.nudesprotectorback.auth.mfa.repository

import mm.nudesprotectorback.auth.mfa.model.MfaOtp
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface MfaOtpRepository : CrudRepository<MfaOtp, UUID>
