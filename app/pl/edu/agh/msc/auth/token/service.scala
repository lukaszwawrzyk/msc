package pl.edu.agh.msc.auth.token

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.util.Clock
import org.joda.time.DateTimeZone

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps

class AuthTokenService @Inject() (
  authTokenRepository: AuthTokenRepository,
  clock:               Clock
)(implicit ex: ExecutionContext) {

  def create(userID: UUID, expiry: FiniteDuration = 5 minutes): Future[AuthToken] = {
    val token = AuthToken(UUID.randomUUID(), userID, clock.now.withZone(DateTimeZone.UTC).plusSeconds(expiry.toSeconds.toInt))
    authTokenRepository.save(token)
  }

  def validate(id: UUID): Future[Option[AuthToken]] = {
    authTokenRepository.find(id)
  }

  def clean(): Future[Seq[AuthToken]] = {
    authTokenRepository.findExpired(clock.now.withZone(DateTimeZone.UTC)).flatMap { tokens =>
      Future.traverse(tokens) { token =>
        authTokenRepository.remove(token.id).map(_ => token)
      }
    }
  }

}
