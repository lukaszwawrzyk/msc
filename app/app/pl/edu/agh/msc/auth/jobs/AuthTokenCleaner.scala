package pl.edu.agh.msc.auth.jobs

import javax.inject.Inject

import akka.actor._
import com.mohiva.play.silhouette.api.util.Clock
import pl.edu.agh.msc.auth.jobs.AuthTokenCleaner.Clean
import pl.edu.agh.msc.auth.token.AuthTokenService
import pl.edu.agh.msc.utils.Logging

import scala.concurrent.ExecutionContext.Implicits.global

class AuthTokenCleaner @Inject() (
  service: AuthTokenService,
  clock:   Clock
) extends Actor with Logging {

  def receive: Receive = {
    case Clean =>
      service.clean()
  }
}

object AuthTokenCleaner {
  case object Clean
}
