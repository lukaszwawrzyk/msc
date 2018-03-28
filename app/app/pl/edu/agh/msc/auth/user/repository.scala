package pl.edu.agh.msc.auth.user

import java.util.UUID
import javax.inject.{ Inject, Singleton }

import com.mohiva.play.silhouette.api.LoginInfo
import pl.edu.agh.msc.utils.SlickTypeMappings
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import pl.edu.agh.msc.utils._
import scala.concurrent.{ ExecutionContext, Future }

@Singleton class UserRepository @Inject() (dbConfigProvider: DatabaseConfigProvider) extends SlickTypeMappings {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  protected val profile = dbConfig.profile
  import dbConfig.db
  import profile.api._

  private case class UserRow(
    providerId:  String,
    providerKey: String,
    firstName:   Option[String],
    lastName:    Option[String],
    email:       Option[String],
    id:          UUID
  )

  private class Users(tag: Tag) extends Table[UserRow](tag, "users") {
    def providerId = column[String]("provider_id")
    def providerKey = column[String]("provider_key")
    def firstName = column[Option[String]]("first_name")
    def lastName = column[Option[String]]("last_name")
    def email = column[Option[String]]("email")
    def id = column[UUID]("id", O.PrimaryKey)
    def * = (providerId, providerKey, firstName, lastName, email, id).mapTo[UserRow]
  }

  private val baseQuery = TableQuery[Users]

  private val byLoginInfoQuery = Compiled { (providerId: Rep[String], providerKey: Rep[String]) =>
    baseQuery.filter(user => user.providerId === providerId && user.providerKey === providerKey)
  }

  def find(loginInfo: LoginInfo): Option[User] = {
    db.run(byLoginInfoQuery((loginInfo.providerID, loginInfo.providerKey)).result.headOption).await().map { userRow =>
      import userRow._
      User(id, loginInfo, firstName, lastName, email)
    }
  }

  def save(user: User): User = {
    val row = UserRow(user.loginInfo.providerID, user.loginInfo.providerKey, user.firstName, user.lastName, user.email, user.id)
    db.run(baseQuery += row).await()
    user
  }

}
