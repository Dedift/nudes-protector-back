package mm.nudesprotectorback.auth.mfa.service

import mm.nudesprotectorback.auth.mfa.model.MfaOtp
import mm.nudesprotectorback.auth.mfa.repository.MfaOtpRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Duration
import java.util.UUID

@Service
class MfaOtpService(
    private val mfaOtpRepository: MfaOtpRepository,
    @Value($$"${app.security.mfa.otp-ttl:PT5M}")
    private val otpTtl: Duration,
    @Value($$"${app.security.mfa.max-attempts:3}")
    private val maxAttempts: Int,
) {
    private val random = SecureRandom()

    fun issueOtp(userId: UUID): String {
        val code = generateOtp()

        mfaOtpRepository.deleteById(userId)
        mfaOtpRepository.save(MfaOtp.newCode(userId, code, otpTtl))
        return code
    }

    fun verifyOtp(userId: UUID, code: String): OtpVerificationResult {
        val storedOtp = mfaOtpRepository.findById(userId).orElse(null) ?: return OtpVerificationResult.EXPIRED

        if (storedOtp.code == code) {
            mfaOtpRepository.deleteById(userId)
            return OtpVerificationResult.SUCCESS
        }

        val nextAttempts = storedOtp.attempts + 1
        return if (nextAttempts >= maxAttempts) {
            mfaOtpRepository.deleteById(userId)
            OtpVerificationResult.REISSUE_REQUIRED
        } else {
            mfaOtpRepository.save(storedOtp.copy(attempts = nextAttempts))
            OtpVerificationResult.INVALID
        }
    }

    private fun generateOtp(): String = (random.nextInt(900_000) + 100_000).toString()
}
