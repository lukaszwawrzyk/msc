package pl.edu.agh.msc.payment

import java.net.URL
import java.util.UUID
import javax.inject.{ Inject, Singleton }

import scala.concurrent.{ ExecutionContext, Future }

trait NotificationService {
  def notifyURL(url: URL): Future[Unit]
}

@Singleton class PaymentService @Inject() (
  notificationService: NotificationService
) {

  def create(payment: PaymentRequest)(implicit ec: ExecutionContext): Future[PaymentId] = {
    Future(PaymentId(UUID.randomUUID()))
  }

  private[payment] def get(id: PaymentId): Future[PaymentRequest] = ???

  private[payment] def pay(id: PaymentId): Future[Unit] = ???

  def isPaid(id: PaymentId): Future[Boolean] = Future(false)

}
