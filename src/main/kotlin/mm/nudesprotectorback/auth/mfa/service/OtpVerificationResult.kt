package mm.nudesprotectorback.auth.mfa.service

enum class OtpVerificationResult {
    SUCCESS,
    INVALID,
    EXPIRED,
    REISSUE_REQUIRED,
}
