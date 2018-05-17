package pl.edu.agh.msc.payment.write

import pl.edu.agh.msc.payment.{ PaymentId, PaymentRequest }
import pl.edu.agh.msc.utils.NotificationService
import pl.edu.agh.msc.utils.cqrs.{ Entity, EntityCompanion }

import scala.reflect.ClassTag

object PaymentEntity extends EntityCompanion {
  override val eventClass = implicitly
  override val commandClass = implicitly
  override def name: String = "payments"
  override def idExtractor: Command => String = _.id.value.toString

  sealed trait Event extends Serializable
  final case class PaymentCreated(id: PaymentId, paymentRequest: PaymentRequest) extends Event
  final case class PaymentCompleted(id: PaymentId) extends Event

  sealed trait Command { def id: PaymentId }
  final case class CreatePayment(id: PaymentId, paymentRequest: PaymentRequest) extends Command
  final case class CompletePayment(id: PaymentId) extends Command

  private case class State(
    request: Option[PaymentRequest],
    isPaid:  Boolean
  ) {
    def create(paymentRequest: PaymentRequest): State = {
      require(request.isEmpty)
      State(Some(paymentRequest), isPaid = false)
    }
    def completed: State = {
      require(!isPaid)
      copy(isPaid = true)
    }
    def returnUrl = request.get.returnUrl
  }

  private object State {
    def initial = State(request = None, isPaid = false)
  }

}

import PaymentEntity._

class PaymentEntity(
  notificationService: NotificationService
) extends Entity[Command, Event] {

  private var state = State.initial

  override val handleCommand = {
    case CreatePayment(id, paymentRequest) =>
      handlePure(PaymentCreated(id, paymentRequest))
    case CompletePayment(id) =>
      handleEffect(PaymentCompleted(id)) {
        notificationService.notifyURL(state.returnUrl)
      }
  }

  override protected val applyEvent = {
    case PaymentCreated(_, paymentRequest) => state = state.create(paymentRequest)
    case PaymentCompleted(_)               => state = state.completed
  }

}
