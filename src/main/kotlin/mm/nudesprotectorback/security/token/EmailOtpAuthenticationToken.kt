package mm.nudesprotectorback.security.token

import org.springframework.security.authentication.AbstractAuthenticationToken

class EmailOtpAuthenticationToken(
    private val email: String,
    private val code: String,
) : AbstractAuthenticationToken(emptyList()) {

    override fun getCredentials(): Any = code

    override fun getPrincipal(): Any = email
}
