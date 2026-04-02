package mm.nudesprotectorback.config

import mm.nudesprotectorback.auth.rememberme.JdbcPersistentTokenRepository
import mm.nudesprotectorback.auth.security.EmailOtpAuthenticationProvider
import mm.nudesprotectorback.auth.security.EmailPasswordAuthenticationProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.security.authentication.ott.JdbcOneTimeTokenService
import org.springframework.security.authentication.ott.OneTimeTokenService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.RememberMeServices
import org.springframework.security.web.authentication.ott.DefaultGenerateOneTimeTokenRequestResolver
import org.springframework.security.web.authentication.ott.GenerateOneTimeTokenRequestResolver
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository

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
    @Value($$"${app.security.ott.ttl:PT5M}")
    private val ottTtl: java.time.Duration,
    @Value($$"${app.security.remember-me.key:change-me-remember-me-key}")
    private val rememberMeKey: String,
    @Value($$"${app.security.remember-me.token-validity-seconds:2592000}")
    private val rememberMeTokenValiditySeconds: Int,
) {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        oneTimeTokenService: OneTimeTokenService,
        generateOneTimeTokenRequestResolver: GenerateOneTimeTokenRequestResolver,
        oneTimeTokenGenerationSuccessHandler: OneTimeTokenGenerationSuccessHandler,
        rememberMeServices: RememberMeServices,
    ): SecurityFilterChain =
        http
            .csrf {
                it.csrfTokenRepository(HttpSessionCsrfTokenRepository())
            }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/csrf",
                    "/logout",
                    "/login/webauthn",
                    "/login/ott",
                    "/ott/generate",
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
            .oneTimeTokenLogin {
                it.tokenGenerationSuccessHandler(oneTimeTokenGenerationSuccessHandler)
                it.tokenService(oneTimeTokenService)
                it.generateRequestResolver(generateOneTimeTokenRequestResolver)
            }
            .rememberMe {
                it.rememberMeServices(rememberMeServices)
            }
            .logout(Customizer.withDefaults())
            .build()

    @Bean
    fun oneTimeTokenService(jdbc: JdbcOperations): OneTimeTokenService = JdbcOneTimeTokenService(jdbc)

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
            setTokenValiditySeconds(rememberMeTokenValiditySeconds)
        }

    @Bean
    fun generateOneTimeTokenRequestResolver(): GenerateOneTimeTokenRequestResolver =
        DefaultGenerateOneTimeTokenRequestResolver().apply {
            setExpiresIn(ottTtl)
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
