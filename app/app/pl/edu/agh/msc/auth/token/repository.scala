package pl.edu.agh.msc.auth.token

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{ Inject, Singleton }

import pl.edu.agh.msc.utils.SlickTypeMappings
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import pl.edu.agh.msc.utils._

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class AuthTokenRepository @Inject() (dbConfigProvider: DatabaseConfigProvider) extends SlickTypeMappings {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  protected val profile = dbConfig.profile
  import dbConfig.db
  import profile.api._

  private case class AuthTokenRow(
    userId: UUID,
    expiry: LocalDateTime,
    id:     UUID,
  )

  private class AuthTokens(tag: Tag) extends Table[AuthTokenRow](tag, "auth_tokens") {
    def userId = column[UUID]("user_id")
    def expiry = column[LocalDateTime]("expiry")
    def id = column[UUID]("id", O.PrimaryKey)
    def * = (userId, expiry, id).mapTo[AuthTokenRow]
  }

  private val baseQuery = TableQuery[AuthTokens]

  private val byIdQuery = Compiled { id: Rep[UUID] =>
    baseQuery.filter(_.id === id)
  }

  private val byExpirationQuery = Compiled { expiresBefore: Rep[LocalDateTime] =>
    baseQuery.filter(_.expiry < expiresBefore)
  }

  def find(id: UUID): Option[AuthToken] = {
    db.run(byIdQuery(id).result.headOption).await().map(convertRow)
  }

  def findExpired(now: LocalDateTime): Seq[AuthToken] = {
    db.run(byExpirationQuery(now).result).await().map(convertRow)
  }

  def save(token: AuthToken): AuthToken = {
    db.run(baseQuery += AuthTokenRow(token.userID, token.expiry, token.id)).await()
    token
  }

  def remove(id: UUID): Unit = db.run {
    byIdQuery(id).delete
  }.await()

  private def convertRow(row: AuthTokenRow) = {
    AuthToken(row.id, row.userId, row.expiry)
  }

}
