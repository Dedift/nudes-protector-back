package mm.nudesprotectorback.auth.rememberme

import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository
import java.sql.ResultSet
import java.util.Date

class JdbcPersistentTokenRepository(
    private val jdbc: JdbcOperations,
) : PersistentTokenRepository {

    override fun createNewToken(token: PersistentRememberMeToken) {
        jdbc.update(
            "insert into persistent_logins (username, series, token, last_used) values (?, ?, ?, ?)",
            token.username,
            token.series,
            token.tokenValue,
            token.date,
        )
    }

    override fun updateToken(
        series: String,
        tokenValue: String,
        lastUsed: Date,
    ) {
        jdbc.update(
            "update persistent_logins set token = ?, last_used = ? where series = ?",
            tokenValue,
            lastUsed,
            series,
        )
    }

    override fun getTokenForSeries(seriesId: String): PersistentRememberMeToken? =
        try {
            jdbc.queryForObject(
                "select username, series, token, last_used from persistent_logins where series = ?",
                { rs: ResultSet, _: Int ->
                    PersistentRememberMeToken(
                        rs.getString("username"),
                        rs.getString("series"),
                        rs.getString("token"),
                        rs.getTimestamp("last_used"),
                    )
                },
                seriesId,
            )
        } catch (_: EmptyResultDataAccessException) {
            null
        }

    override fun removeUserTokens(username: String) {
        jdbc.update(
            "delete from persistent_logins where username = ?",
            username,
        )
    }
}
