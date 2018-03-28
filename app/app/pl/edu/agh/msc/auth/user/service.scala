package pl.edu.agh.msc.auth.user

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import javax.inject.Inject

import scala.concurrent.Future

class UserService @Inject() (
  userRepository: UserRepository
) extends IdentityService[User] {

  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = Future.successful(userRepository.find(loginInfo))

  def save(user: User): Future[User] = Future.successful(userRepository.save(user))

}
