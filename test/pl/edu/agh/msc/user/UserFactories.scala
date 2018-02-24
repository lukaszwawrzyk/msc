package pl.edu.agh.msc.user

import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import pl.edu.agh.msc.auth.user.{ User, UserService }
import pl.edu.agh.msc.common.IntegrationTest
import cats.syntax.option._

import scala.concurrent.Future

trait UserFactories { this: IntegrationTest =>

  private val userService = inject[UserService]

  def createAndSaveUser(): Future[UUID] = {
    val id = UUID.randomUUID()
    val user = User(
      id,
      LoginInfo(CredentialsProvider.ID, "a@b.com"),
      "John".some,
      "Doe".some,
      "a@b.com".some
    )
    userService.save(user).map(_.id)
  }

}