package pl.edu.agh.msc.auth.user

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService

import scala.concurrent.{ ExecutionContext, Future }

class UserService @Inject() (
  userRepository: UserRepository
)(implicit ex: ExecutionContext) extends IdentityService[User] {

  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = userRepository.find(loginInfo)

  def save(user: User): Future[User] = userRepository.save(user)

}
