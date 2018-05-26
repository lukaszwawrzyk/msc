package pl.edu.agh.msc.payment

import java.util.UUID

import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.payment.read.{ PaymentEventMapper, PaymentRepository }
import pl.edu.agh.msc.payment.write.{ PaymentEntitiesFacade, PaymentEntity }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

@Singleton class PaymentService @Inject() (
  entitiesFacade: PaymentEntitiesFacade,
  eventMapper:    PaymentEventMapper
) {

  eventMapper.run()

  def create(payment: PaymentRequest)(implicit ec: ExecutionContext): Future[PaymentId] = {
    val id = PaymentId(UUID.randomUUID())
    entitiesFacade.command(PaymentEntity.CreatePayment(id, payment)).map(_ => id)
  }

  def get(id: PaymentId)(implicit ec: ExecutionContext): Future[PaymentRequest] = {
    entitiesFacade.query[Option[PaymentRequest]](PaymentEntity.GetPayment(id)).map(_.get)
  }

  def pay(id: PaymentId)(implicit ec: ExecutionContext): Future[Unit] = {
    entitiesFacade.command(PaymentEntity.CompletePayment(id))
  }

}
