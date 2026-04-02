package mm.nudesprotectorback.auth.oauth2.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table(name = "user_oauth2_accounts")
data class UserOAuth2Account(
    @Id
    val id: UUID? = null,
    val provider: String,
    @Column("provider_user_id")
    val providerUserId: String,
    @Column("user_id")
    val userId: UUID,
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
)
