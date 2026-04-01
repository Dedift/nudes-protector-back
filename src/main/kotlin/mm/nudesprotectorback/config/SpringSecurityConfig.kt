package mm.nudesprotectorback.config

import mm.nudesprotectorback.security.provider.EmailOtpAuthenticationProvider
import mm.nudesprotectorback.security.provider.EmailPasswordAuthenticationProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SpringSecurityConfig {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
    ): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/logout",
                    "/users/register",
                    "/users/verify-email",
                    "/users/mfa/login",
                    "/users/mfa/verify",
                ).permitAll()
                it.anyRequest().authenticated()
            }
            .build()

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
