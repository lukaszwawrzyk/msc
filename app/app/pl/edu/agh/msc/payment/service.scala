package pl.edu.agh.msc.payment

import java.net.URL
import java.util.UUID
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

@Singleton class PaymentService @Inject() (
  notificationService: NotificationService,
  paymentRepository:   PaymentRepository
) {

  def create(payment: PaymentRequest)(implicit ec: ExecutionContext): Future[PaymentId] = {
    val id = PaymentId(UUID.randomUUID())
    paymentRepository.insert(id, payment).map(_ => id)
  }

  def isPaid(id: PaymentId)(implicit ec: ExecutionContext): Future[Boolean] = {
    paymentRepository.getPaymentStatus(id)
  }

  // internal stuff

  def get(id: PaymentId)(implicit ec: ExecutionContext): Future[PaymentRequest] = {
    paymentRepository.find(id)
  }

  def pay(id: PaymentId)(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      _ <- paymentRepository.setPaymentStatus(id, isPaid = true)
      payment <- paymentRepository.find(id)
      _ <- notificationService.notifyURL(payment.returnUrl)
    } yield ()
  }

}
