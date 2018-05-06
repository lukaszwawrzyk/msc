package pl.edu.agh.msc.orders.write

import java.util.UUID

import akka.pattern._
import akka.persistence.PersistentActor
import akka.persistence.journal.{ EventSeq, Tagged }
import pl.edu.agh.msc.cart.CartService
import pl.edu.agh.msc.orders._
import pl.edu.agh.msc.orders.write.OrderEntity._
import pl.edu.agh.msc.products.ProductService
import pl.edu.agh.msc.utils.Time

import scala.collection.immutable.Set
import scala.concurrent.Future

case class State(
  order: Option[Order]
) {
  def withStatus(status: OrderStatus.Value): State = {
    require(status != order.get.status)
    State(order.map(_.copy(status = status)))
  }
}

object State {
  def notCreated = State(None)
  def create(order: Order) = State(Some(order))
}

object OrderEntity {
  sealed trait Event extends Serializable
  final case class OrderCreated(order: Order) extends Event
  final case class OrderConfirmed(id: OrderId) extends Event
  final case class OrderPaid(id: OrderId) extends Event

  sealed trait Command
  final case class CreateOrder(orderDraft: OrderDraft, user: UUID) extends Command
  final case class ConfirmOrder() extends Command
  final case class PayOrder() extends Command

  private case class DelayedResult(value: Any)

  case object Ack

}

class OrderEntity(
  id:             OrderId,
  productService: ProductService,
  cartService:    CartService,
  time:           Time
) extends PersistentActor {

  import context.dispatcher

  def persistenceId: String = id.toString

  private var state = State.notCreated

  override def receiveCommand: Receive = {
    case CreateOrder(draft, user) =>
      handleDelayed(createInitialOrder(draft, user))(OrderCreated(_))(sideEffect = cartService.clear(user))(identity)
    case ConfirmOrder() =>
      handlePure(OrderConfirmed(id))
    case PayOrder() =>
      handlePure(OrderPaid(id))
  }

  private def handlePure(event: Event): Unit = {
    val persistentSender = sender()
    emit(event) { e =>
      applyEvent(e)
      persistentSender ! Ack
    }
  }

  private def handleDelayed[A](initialLogic: Future[A])(event: A => Event)(sideEffect: => Unit)(response: A => Any): Unit = {
    val persistentSender = sender()
    initialLogic.map(DelayedResult) pipeTo self
    context.become({
      case DelayedResult(res: A @unchecked) =>
        emit(event(res)) { e =>
          applyEvent(e)
          sideEffect
          persistentSender ! response(res)
        }
        unstashAll()
        context.unbecome()
      case _ => stash()
    }, discardOld = false)
  }

  override def receiveRecover: Receive = {
    case event: Event =>
      applyEvent(event)
    case t @ Tagged(event: Event, _) =>
      applyEvent(event)
  }

  private val applyEvent: Event => Unit = {
    case OrderCreated(order) => state = State.create(order)
    case OrderConfirmed(_)   => state = state.withStatus(OrderStatus.Confirmed)
    case OrderPaid(_)        => state = state.withStatus(OrderStatus.Paid)
  }

  private def createInitialOrder(draft: OrderDraft, user: UUID): Future[Order] = {
    for {
      items <- Future.traverse(draft.cart.items) { cartItem =>
        productService.price(cartItem.product).map { price =>
          LineItem(cartItem.product, cartItem.amount, price)
        }
      }
      order = Order(
        id,
        buyer   = user,
        address = draft.address,
        status  = OrderStatus.Unconfirmed,
        items,
        date = time.now()
      )
    } yield {
      order
    }
  }

  private def emit[A](event: A)(handler: A => Unit): Unit = {
    persist(event)(e => {
      handler(e)
    })
  }
}
