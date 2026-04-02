package mm.nudesprotectorback.auth.oauth2.service

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class CustomOAuth2UserService(
    private val oAuth2AccountLinkingService: OAuth2AccountLinkingService,
) : OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private val delegate = DefaultOAuth2UserService()
    private val restClient = RestClient.builder()
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build()

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oauth2User = delegate.loadUser(userRequest)
        val registrationId = userRequest.clientRegistration.registrationId.lowercase()
        val providerUserId = oauth2User.name
        val email = resolveEmail(userRequest, oauth2User)
        val username = resolveUsername(oauth2User, email)
        val localUser = oAuth2AccountLinkingService.loadOrCreateUser(
            provider = registrationId,
            providerUserId = providerUserId,
            email = email,
            usernameCandidate = username,
        )

        val attributes = LinkedHashMap(oauth2User.attributes)
        attributes["email"] = localUser.email
        attributes["local_user_id"] = checkNotNull(localUser.id).toString()

        return DefaultOAuth2User(
            mergeAuthorities(oauth2User.authorities),
            attributes,
            "email",
        )
    }

    private fun resolveEmail(
        userRequest: OAuth2UserRequest,
        oauth2User: OAuth2User,
    ): String {
        val directEmail = oauth2User.attributes["email"]?.toString()?.trim()
        if (!directEmail.isNullOrBlank()) {
            return directEmail
        }

        if (userRequest.clientRegistration.registrationId.equals("github", ignoreCase = true)) {
            return loadGithubEmail(userRequest)
        }

        throw OAuth2AuthenticationException(
            OAuth2Error("missing_email", "OAuth2 provider did not return user email", null),
        )
    }

    private fun loadGithubEmail(userRequest: OAuth2UserRequest): String {
        val emails = restClient.get()
            .uri("https://api.github.com/user/emails")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${userRequest.accessToken.tokenValue}")
            .retrieve()
            .body(Array<GithubEmailResponse>::class.java)
            ?.toList()
            .orEmpty()

        return emails.firstOrNull { it.primary && it.verified }?.email
            ?: emails.firstOrNull { it.verified }?.email
            ?: throw OAuth2AuthenticationException(
                OAuth2Error("missing_email", "GitHub did not return a verified email", null),
            )
    }

    private fun resolveUsername(
        oauth2User: OAuth2User,
        email: String,
    ): String =
        oauth2User.attributes["name"]?.toString()?.trim().takeUnless { it.isNullOrBlank() }
            ?: oauth2User.attributes["login"]?.toString()?.trim().takeUnless { it.isNullOrBlank() }
            ?: email.substringBefore("@")

    private fun mergeAuthorities(authorities: Collection<GrantedAuthority>): Set<GrantedAuthority> =
        linkedSetOf<GrantedAuthority>(SimpleGrantedAuthority("ROLE_USER")).apply {
            addAll(authorities)
        }

    private data class GithubEmailResponse(
        val email: String,
        val primary: Boolean,
        val verified: Boolean,
    )
}
