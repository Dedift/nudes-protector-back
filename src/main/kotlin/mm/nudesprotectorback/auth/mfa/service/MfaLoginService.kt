package mm.nudesprotectorback.auth.mfa.service

import mm.nudesprotectorback.mail.MailService
import mm.nudesprotectorback.user.model.User
import org.springframework.stereotype.Service

@Service
class MfaLoginService(
    private val mfaOtpService: MfaOtpService,
    private val mailService: MailService,
) {
    fun issueOtp(user: User) {
        val code = mfaOtpService.issueOtp(checkNotNull(user.id))

        mailService.sendTextMail(
            email = user.email,
            subject = "Your login code",
            text = "Your login code is: $code",
        )
    }
}
