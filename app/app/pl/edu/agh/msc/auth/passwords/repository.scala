package pl.edu.agh.msc.auth.passwords

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import pl.edu.agh.msc.utils._
import scala.concurrent.{ ExecutionContext, Future }

class PasswordRepository @Inject() (dbConfigProvider: DatabaseConfigProvider) extends DelegableAuthInfoDAO[PasswordInfo] {

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

  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = Future.successful {
    db.run(byIdQuery((loginInfo.providerID, loginInfo.providerKey)).result.headOption).await().map(toPasswordInfo)
  }

  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = Future.successful {
    val row = toRow(loginInfo, authInfo)
    db.run(baseQuery += row).await()
    authInfo
  }

  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = Future.successful {
    val row = toRow(loginInfo, authInfo)
    db.run(byIdQuery((loginInfo.providerID, loginInfo.providerKey)).update(row)).await()
    authInfo
  }

  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    val exists = db.run(existsByIdQuery((loginInfo.providerID, loginInfo.providerKey)).result).await()
    if (exists) {
      add(loginInfo, authInfo)
    } else {
      save(loginInfo, authInfo)
    }
  }

  override def remove(loginInfo: LoginInfo): Future[Unit] = Future.successful {
    db.run(byIdQuery((loginInfo.providerID, loginInfo.providerKey)).delete).await()
  }

  private def toRow(loginInfo: LoginInfo, authInfo: PasswordInfo) = {
    PasswordRow(loginInfo.providerID, loginInfo.providerKey, authInfo.hasher, authInfo.password, authInfo.salt)
  }

  private def toPasswordInfo(row: PasswordRow) = {
    PasswordInfo(row.hasher, row.password, row.salt)
  }

}
