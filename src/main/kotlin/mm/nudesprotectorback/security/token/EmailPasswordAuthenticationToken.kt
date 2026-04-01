package mm.nudesprotectorback.security.token

import org.springframework.security.authentication.AbstractAuthenticationToken

class EmailPasswordAuthenticationToken(
    private val email: String,
    private val password: String,
) : AbstractAuthenticationToken(emptyList()) {

    override fun getCredentials(): Any = password

    override fun getPrincipal(): Any = email
}
