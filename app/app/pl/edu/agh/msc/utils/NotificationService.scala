package pl.edu.agh.msc.utils

import java.net.URL

import javax.inject.{ Inject, Singleton }
import play.api.libs.ws.WSClient

import scala.concurrent.{ ExecutionContext, Future }

trait NotificationService {
  def notifyURL(url: URL): Future[Unit]
}

@Singleton class WSNotificationService @Inject() (
  ws: WSClient
)(implicit ec: ExecutionContext) extends NotificationService {

  override def notifyURL(url: URL): Future[Unit] = {
    ws.url(url.toString).post("").map(_ => ())
  }

}
