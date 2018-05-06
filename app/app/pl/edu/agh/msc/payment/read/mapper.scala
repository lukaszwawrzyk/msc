package pl.edu.agh.msc.payment.read

import akka.actor.ActorSystem
import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.payment.write.PaymentEntity
import pl.edu.agh.msc.utils.cqrs.{ EventMapper, OffsetRepository }

@Singleton class PaymentEventMapper @Inject() (
  system:            ActorSystem,
  offsetRepository:  OffsetRepository,
  paymentRepository: PaymentRepository
) extends EventMapper(PaymentEntity, system, offsetRepository) {

  import system.dispatcher

  override protected val process = {
    case PaymentEntity.PaymentCreated(id, request) => paymentRepository.insert(id, request)
    case PaymentEntity.PaymentCompleted(id)        => paymentRepository.setPaymentStatus(id, isPaid = true)
  }

}
