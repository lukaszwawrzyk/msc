package pl.edu.agh.msc.auth.token

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps

class AuthTokenService @Inject() (
  authTokenRepository: AuthTokenRepository
) {

  def create(userId: UUID, expiry: FiniteDuration = 5 minutes)(implicit ec: ExecutionContext): Future[AuthToken] = {
    val token = AuthToken(UUID.randomUUID(), userId, LocalDateTime.now().plusSeconds(expiry.toSeconds))
    authTokenRepository.save(token)
  }

  def validate(id: UUID)(implicit ec: ExecutionContext): Future[Option[AuthToken]] = {
    authTokenRepository.find(id)
  }

  def clean()(implicit ec: ExecutionContext): Future[Seq[AuthToken]] = {
    authTokenRepository.findExpired(LocalDateTime.now()).flatMap { tokens =>
      Future.traverse(tokens) { token =>
        authTokenRepository.remove(token.id).map(_ => token)
      }
    }
  }

}
