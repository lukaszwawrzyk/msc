package pl.edu.agh.msc.auth.user

import java.util.UUID

import com.mohiva.play.silhouette.api.LoginInfo

import scala.collection.mutable
import scala.concurrent.Future

class UserRepository {
  private val users: mutable.HashMap[UUID, User] = mutable.HashMap()

  def find(loginInfo: LoginInfo): Future[Option[User]] = Future.successful {
    users.find { case (_, user) => user.loginInfo == loginInfo }.map(_._2)
  }

  def save(user: User): Future[User] = {
    users += (user.userID -> user)
    Future.successful(user)
  }
}
