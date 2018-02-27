package pl.edu.agh.msc.payment

import java.net.URL
import java.util.UUID
import javax.inject.{ Inject, Singleton }

import scala.concurrent.{ ExecutionContext, Future }

trait NotificationService {
  def notifyURL(url: URL): Future[Unit]
}

@Singleton class PaymentService @Inject() (
  notificationService: NotificationService,
  paymentRepository:   PaymentRepository
) {

  def create(payment: PaymentRequest)(implicit ec: ExecutionContext): Future[PaymentId] = {
    val id = PaymentId(UUID.randomUUID())
    paymentRepository.insert(id, payment).map(_ => id)
  }

  private[payment] def get(id: PaymentId)(implicit ec: ExecutionContext): Future[PaymentRequest] = {
    paymentRepository.find(id)
  }

  private[payment] def pay(id: PaymentId)(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      _ <- paymentRepository.setPaymentStatus(id, isPaid = true)
      payment <- paymentRepository.find(id)
      _ <- notificationService.notifyURL(payment.returnUrl)
    } yield ()
  }

  def isPaid(id: PaymentId)(implicit ec: ExecutionContext): Future[Boolean] = {
    paymentRepository.getPaymentStatus(id)
  }

}
