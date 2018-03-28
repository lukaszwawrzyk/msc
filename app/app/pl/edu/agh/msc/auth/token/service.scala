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

  def create(userId: UUID, expiry: FiniteDuration = 5 minutes): AuthToken = {
    val token = AuthToken(UUID.randomUUID(), userId, LocalDateTime.now().plusSeconds(expiry.toSeconds))
    authTokenRepository.save(token)
  }

  def validate(id: UUID): Option[AuthToken] = {
    authTokenRepository.find(id)
  }

  def clean(): Seq[AuthToken] = {
    val tokens = authTokenRepository.findExpired(LocalDateTime.now())
    tokens.foreach(token => authTokenRepository.remove(token.id))
    tokens
  }

}
