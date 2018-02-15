package pl.edu.agh.msc.auth.token

import java.util.UUID

import org.joda.time.DateTime

import scala.collection.mutable
import scala.concurrent.Future

class AuthTokenRepository {

  private val tokens: mutable.HashMap[UUID, AuthToken] = mutable.HashMap()

  def find(id: UUID): Future[Option[AuthToken]] = {
    Future.successful(tokens.get(id))
  }

  def findExpired(dateTime: DateTime): Future[Seq[AuthToken]] = Future.successful {
    tokens.filter {
      case (_, token) =>
        token.expiry.isBefore(dateTime)
    }.values.toSeq
  }

  def save(token: AuthToken): Future[AuthToken] = {
    tokens += (token.id -> token)
    Future.successful(token)
  }

  def remove(id: UUID): Future[Unit] = {
    tokens -= id
    Future.successful(())
  }

}
