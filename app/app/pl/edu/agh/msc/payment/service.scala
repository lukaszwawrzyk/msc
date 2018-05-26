package pl.edu.agh.msc.payment

import java.util.UUID

import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.payment.read.{ PaymentEventMapper, PaymentRepository }
import pl.edu.agh.msc.payment.write.{ PaymentEntitiesFacade, PaymentEntity }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

@Singleton class PaymentService @Inject() (
  paymentRepository: PaymentRepository,
  entitiesFacade:    PaymentEntitiesFacade,
  eventMapper:       PaymentEventMapper
) {

  eventMapper.run()

  def create(payment: PaymentRequest)(implicit ec: ExecutionContext): Future[PaymentId] = {
    val id = PaymentId(UUID.randomUUID())
    entitiesFacade.call(PaymentEntity.CreatePayment(id, payment)).map(_ => id)
  }

  def isPaid(id: PaymentId)(implicit ec: ExecutionContext): Future[Boolean] = {
    paymentRepository.getPaymentStatus(id)
  }

  def exists(id: PaymentId)(implicit ec: ExecutionContext): Future[Boolean] = {
    isPaid(id).map(_ => true).recover { case NonFatal(x) => false }
  }

  // internal stuff

  def get(id: PaymentId)(implicit ec: ExecutionContext): Future[PaymentRequest] = {
    paymentRepository.find(id)
  }

  def pay(id: PaymentId)(implicit ec: ExecutionContext): Future[Unit] = {
    entitiesFacade.call(PaymentEntity.CompletePayment(id))
  }

}
