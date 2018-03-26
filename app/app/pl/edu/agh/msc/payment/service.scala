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
) extends NotificationService {

  override def notifyURL(url: URL): Future[Unit] = {
    ws.url(url.toString).post("").map(_ => ())
  }

}

@Singleton class PaymentService @Inject() (
  notificationService: NotificationService,
  paymentRepository:   PaymentRepository
) {

  def create(payment: PaymentRequest): PaymentId = {
    val id = PaymentId(UUID.randomUUID())
    paymentRepository.insert(id, payment)
    id
  }

  def isPaid(id: PaymentId): Boolean = {
    paymentRepository.getPaymentStatus(id)
  }

  // internal stuff

  def get(id: PaymentId): PaymentRequest = {
    paymentRepository.find(id)
  }

  def pay(id: PaymentId): Unit = {
    paymentRepository.setPaymentStatus(id, isPaid = true)
    val payment = paymentRepository.find(id)
    notificationService.notifyURL(payment.returnUrl)
  }

}
