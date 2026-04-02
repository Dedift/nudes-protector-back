package mm.nudesprotectorback.auth.oauth2.service

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.stereotype.Service

@Service
class CustomOidcUserService(
    private val oAuth2AccountLinkingService: OAuth2AccountLinkingService,
) : OAuth2UserService<OidcUserRequest, OidcUser> {
    private val delegate = OidcUserService()

    override fun loadUser(userRequest: OidcUserRequest): OidcUser {
        val oidcUser = delegate.loadUser(userRequest)
        val email = requireNotNull(oidcUser.email).trim().lowercase()
        val localUser = oAuth2AccountLinkingService.loadOrCreateUser(
            provider = userRequest.clientRegistration.registrationId,
            providerUserId = oidcUser.subject,
            email = email,
            usernameCandidate = oidcUser.fullName ?: oidcUser.givenName ?: email.substringBefore("@"),
        )

        val attributes = LinkedHashMap(oidcUser.attributes)
        attributes["email"] = localUser.email
        attributes["local_user_id"] = checkNotNull(localUser.id).toString()

        return DefaultOidcUser(
            mergeAuthorities(oidcUser.authorities),
            oidcUser.idToken,
            OidcUserInfo(attributes),
            "email",
        )
    }

    private fun mergeAuthorities(authorities: Collection<GrantedAuthority>): Set<GrantedAuthority> =
        linkedSetOf<GrantedAuthority>(SimpleGrantedAuthority("ROLE_USER")).apply {
            addAll(authorities)
        }
}
