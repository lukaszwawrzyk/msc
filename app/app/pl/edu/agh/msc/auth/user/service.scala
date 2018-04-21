package pl.edu.agh.msc.auth.user

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import javax.inject.Inject
import pl.edu.agh.msc.utils.Cache

import scala.concurrent.{ ExecutionContext, Future }

class UserService @Inject() (
  userRepository: UserRepository,
  cache:          Cache
)(implicit ex: ExecutionContext) extends IdentityService[User] {

  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = {
    cache.cachedOpt(namespace = "user")(loginInfo)(userRepository.find)
  }

  def save(user: User): Future[User] = userRepository.save(user)

}
