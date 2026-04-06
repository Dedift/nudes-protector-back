package mm.nudesprotectorback.auth.ott.repository

import mm.nudesprotectorback.auth.ott.model.OttToken
import org.springframework.data.repository.CrudRepository

interface OttTokenRepository : CrudRepository<OttToken, String>
