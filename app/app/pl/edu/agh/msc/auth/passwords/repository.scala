package pl.edu.agh.msc.auth.passwords

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }

class PasswordRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends DelegableAuthInfoDAO[PasswordInfo] {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  protected val profile = dbConfig.profile
  import dbConfig.db
  import profile.api._

  private case class PasswordRow(
    providerId:  String,
    providerKey: String,
    hasher:      String,
    password:    String,
    salt:        Option[String]
  )

  private class Passwords(tag: Tag) extends Table[PasswordRow](tag, "passwords") {
    def providerId = column[String]("provider_id")
    def providerKey = column[String]("provider_key")
    def hasher = column[String]("hasher")
    def password = column[String]("password")
    def salt = column[Option[String]]("salt")
    def pk = primaryKey("passwords_pk", (providerId, providerKey))
    def * = (providerId, providerKey, hasher, password, salt).mapTo[PasswordRow]
  }

  private val baseQuery = TableQuery[Passwords]

  private def byIdQueryFunc(providerId: Rep[String], providerKey: Rep[String]) = {
    baseQuery.filter(pass => pass.providerId === providerId && pass.providerKey === providerKey)
  }

  private val byIdQuery = Compiled { (providerId: Rep[String], providerKey: Rep[String]) =>
    byIdQueryFunc(providerId, providerKey)
  }

  private val existsByIdQuery = Compiled { (providerId: Rep[String], providerKey: Rep[String]) =>
    byIdQueryFunc(providerId, providerKey).exists
  }

  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = db.run {
    byIdQuery((loginInfo.providerID, loginInfo.providerKey)).result.headOption.map(_.map(toPasswordInfo))
  }

  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = db.run {
    val row = toRow(loginInfo, authInfo)
    (baseQuery += row).map(_ => authInfo)
  }

  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = db.run {
    val row = toRow(loginInfo, authInfo)
    byIdQuery((loginInfo.providerID, loginInfo.providerKey)).update(row).map(_ => authInfo)
  }

  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    db.run(existsByIdQuery((loginInfo.providerID, loginInfo.providerKey)).result).flatMap { exists =>
      if (exists) {
        add(loginInfo, authInfo)
      } else {
        save(loginInfo, authInfo)
      }
    }
  }

  override def remove(loginInfo: LoginInfo): Future[Unit] = db.run {
    DBIO.seq(byIdQuery((loginInfo.providerID, loginInfo.providerKey)).delete)
  }

  private def toRow(loginInfo: LoginInfo, authInfo: PasswordInfo) = {
    PasswordRow(loginInfo.providerID, loginInfo.providerKey, authInfo.hasher, authInfo.password, authInfo.salt)
  }

  private def toPasswordInfo(row: PasswordRow) = {
    PasswordInfo(row.hasher, row.password, row.salt)
  }

}
