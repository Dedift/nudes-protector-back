package mm.nudesprotectorback.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SpringSecurityConfig {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        userDetailsService: UserDetailsService,
    ): SecurityFilterChain =
        http
            .userDetailsService(userDetailsService)
            .cors(Customizer.withDefaults())
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/login",
                    "/logout",
                    "/login/webauthn",
                    "/users/register",
                    "/users/verify-email",
                    "/users/mfa/login",
                    "/users/mfa/verify",
                    "/webauthn/authenticate/options",
                ).permitAll()
                it.requestMatchers(HttpMethod.DELETE, "/webauthn/register/**").denyAll()
                it.anyRequest().authenticated()
            }
            .formLogin(Customizer.withDefaults())
            .logout(Customizer.withDefaults())
            .build()

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)
}
