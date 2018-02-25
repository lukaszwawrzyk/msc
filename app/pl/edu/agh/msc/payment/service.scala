package pl.edu.agh.msc.payment

import java.util.UUID

import scala.concurrent.{ ExecutionContext, Future }

class PaymentService {

  def create(payment: Payment)(implicit ec: ExecutionContext): Future[PaymentId] = {
    Future(PaymentId(UUID.randomUUID()))
  }

  private[payment] def get(id: PaymentId): Future[Payment] = ???

  private[payment] def pay(id: PaymentId): Future[Unit] = ???

  def isPaid(id: PaymentId): Future[Boolean] = Future(false)

}
