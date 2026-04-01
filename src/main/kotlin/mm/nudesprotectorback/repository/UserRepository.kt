package mm.nudesprotectorback.repository

import mm.nudesprotectorback.domain.User
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface UserRepository : CrudRepository<User, UUID> {
    fun existsByEmailIgnoreCase(email: String): Boolean
    fun findByEmailIgnoreCase(email: String): User?
}
