package mm.nudesprotectorback.auth.loginattempt.repository

import mm.nudesprotectorback.auth.loginattempt.model.LoginAttemptState
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface LoginAttemptStateRepository : CrudRepository<LoginAttemptState, UUID>
