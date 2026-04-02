package mm.nudesprotectorback.auth.oauth2.repository

import mm.nudesprotectorback.auth.oauth2.model.UserOAuth2Account
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface UserOAuth2AccountRepository : CrudRepository<UserOAuth2Account, UUID> {
    fun findByProviderAndProviderUserId(provider: String, providerUserId: String): UserOAuth2Account?
    fun findByUserIdAndProvider(userId: UUID, provider: String): UserOAuth2Account?
}
