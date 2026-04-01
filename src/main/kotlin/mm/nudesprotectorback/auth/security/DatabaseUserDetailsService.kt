package mm.nudesprotectorback.auth.security

import mm.nudesprotectorback.user.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class DatabaseUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val normalizedEmail = username.trim().lowercase()
        val user = userRepository.findByEmailIgnoreCase(normalizedEmail)
            ?: throw UsernameNotFoundException("User with email '$normalizedEmail' not found")

        return User.builder()
            .username(user.email)
            .password(user.passwordHash)
            .authorities(SimpleGrantedAuthority("ROLE_USER"))
            .disabled(!user.emailVerified)
            .build()
    }
}
