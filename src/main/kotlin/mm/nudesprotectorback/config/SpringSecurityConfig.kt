package mm.nudesprotectorback.config

import mm.nudesprotectorback.auth.oauth2.service.CustomOAuth2UserService
import mm.nudesprotectorback.auth.oauth2.service.CustomOidcUserService
import mm.nudesprotectorback.auth.rememberme.JdbcPersistentTokenRepository
import mm.nudesprotectorback.auth.security.EmailOtpAuthenticationProvider
import mm.nudesprotectorback.auth.security.EmailPasswordAuthenticationProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.server.servlet.CookieSameSiteSupplier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.RememberMeServices
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository
import org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SpringSecurityConfig(
    @Value($$"${app.security.passkeys.rp-id:localhost}")
    private val passkeyRpId: String,
    @Value($$"${app.security.passkeys.rp-name:Nudes Protector}")
    private val passkeyRpName: String,
    @Value($$"${app.security.passkeys.allowed-origins:http://localhost:3000}")
    private val allowedOrigins: String,
    @Value($$"${app.frontend.base-url:http://localhost:3000}")
    private val frontendBaseUrl: String,
    @Value($$"${app.security.cors.allowed-origins:}")
    private val corsAllowedOrigins: String,
    @Value($$"${app.security.remember-me.key:change-me-remember-me-key}")
    private val rememberMeKey: String,
    @Value($$"${app.security.remember-me.token-validity-seconds:2592000}")
    private val rememberMeTokenValiditySeconds: Int,
    @Value($$"${app.security.remember-me.cookie-name:remember-me}")
    private val rememberMeCookieName: String,
    @Value($$"${app.security.remember-me.secure-cookie:false}")
    private val rememberMeSecureCookie: Boolean,
) {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        rememberMeServices: RememberMeServices,
        persistentTokenRepository: PersistentTokenRepository,
        customOAuth2UserService: CustomOAuth2UserService,
        customOidcUserService: CustomOidcUserService,
        corsConfigurationSource: CorsConfigurationSource,
    ): SecurityFilterChain =
        http
            .cors {
                it.configurationSource(corsConfigurationSource)
            }
            .csrf {
                it.csrfTokenRepository(HttpSessionCsrfTokenRepository())
                it.ignoringRequestMatchers(
                    "/users/register",
                    "/users/verify-email",
                    "/users/mfa/login",
                    "/users/mfa/verify",
                    "/ott/generate",
                    "/login/webauthn",
                    "/webauthn/register",
                    "/webauthn/register/options",
                    "/webauthn/authenticate/options",
                    "/oauth2/**",
                    "/login/oauth2/**",
                )
            }
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, _ ->
                    response.status = HttpStatus.UNAUTHORIZED.value()
                }
                it.accessDeniedHandler { _, response, _ ->
                    response.status = HttpStatus.FORBIDDEN.value()
                }
            }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/csrf",
                    "/api/images",
                    "/api/images/*/blurred",
                    "/logout",
                    "/oauth2/**",
                    "/login/webauthn",
                    "/login/oauth2/**",
                    "/ott/generate",
                    "/api/login/ott",
                    "/users/register",
                    "/users/verify-email",
                    "/users/mfa/login",
                    "/users/mfa/verify",
                    "/webauthn/authenticate/options",
                ).permitAll()
                it.anyRequest().authenticated()
            }
            .webAuthn {
                it.rpId(passkeyRpId)
                it.rpName(passkeyRpName)
                it.allowedOrigins(allowedOrigins)
                it.disableDefaultRegistrationPage(true)
            }
            .rememberMe {
                it.rememberMeServices(rememberMeServices)
            }
            .oauth2Login {
                it.userInfoEndpoint { userInfo ->
                    userInfo.userService(customOAuth2UserService)
                    userInfo.oidcUserService(customOidcUserService)
                }
                it.successHandler { _, response, _ ->
                    response.sendRedirect(frontendBaseUrl)
                }
                it.failureHandler { _, response, exception ->
                    response.sendRedirect("$frontendBaseUrl?screen=login&error=${exception.javaClass.simpleName}")
                }
            }
            .logout {
                it.invalidateHttpSession(true)
                it.clearAuthentication(true)
                it.deleteCookies("JSESSIONID", rememberMeCookieName)
                it.addLogoutHandler { _, _, authentication ->
                    authentication?.name?.let(persistentTokenRepository::removeUserTokens)
                }
                it.logoutSuccessHandler { _, response, _ ->
                    response.status = 204
                }
            }
            .build()

    @Bean
    fun persistentTokenRepository(jdbc: JdbcOperations): PersistentTokenRepository =
        JdbcPersistentTokenRepository(jdbc)

    @Bean
    fun rememberMeServices(
        userDetailsService: UserDetailsService,
        persistentTokenRepository: PersistentTokenRepository,
    ): RememberMeServices =
        PersistentTokenBasedRememberMeServices(
            rememberMeKey,
            userDetailsService,
            persistentTokenRepository,
        ).apply {
            setCookieName(rememberMeCookieName)
            setTokenValiditySeconds(rememberMeTokenValiditySeconds)
            setUseSecureCookie(rememberMeSecureCookie)
        }

    @Bean
    fun rememberMeCookieSameSiteSupplier(): CookieSameSiteSupplier =
        CookieSameSiteSupplier.ofLax().whenHasName(rememberMeCookieName)

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource =
        UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration(
                "/**",
                CorsConfiguration().apply {
                    allowedOriginPatterns = resolvedCorsAllowedOrigins()
                    allowedMethods = listOf("*")
                    allowedHeaders = listOf("*")
                    allowCredentials = true
                }
            )
        }

    private fun resolvedCorsAllowedOrigins(): List<String> {
        val configuredOrigins = corsAllowedOrigins
            .split(",")
            .map(String::trim)
            .filter(String::isNotEmpty)

        return configuredOrigins.ifEmpty {
            listOf(frontendBaseUrl)
        }
    }

    @Bean
    fun jdbcPublicKeyCredentialRepository(jdbc: JdbcOperations): JdbcPublicKeyCredentialUserEntityRepository {
        return JdbcPublicKeyCredentialUserEntityRepository(jdbc)
    }

    @Bean
    fun jdbcUserCredentialRepository(jdbc: JdbcOperations): JdbcUserCredentialRepository {
        return JdbcUserCredentialRepository(jdbc)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)

    @Bean
    fun authenticationManager(
        emailPasswordAuthenticationProvider: EmailPasswordAuthenticationProvider,
        emailOtpAuthenticationProvider: EmailOtpAuthenticationProvider,
    ): AuthenticationManager = ProviderManager(
        emailPasswordAuthenticationProvider,
        emailOtpAuthenticationProvider,
    )
}
