package pl.edu.agh.msc.orders.write

import java.util.UUID

import akka.pattern._
import akka.persistence.PersistentActor
import pl.edu.agh.msc.cart.CartService
import pl.edu.agh.msc.orders._
import pl.edu.agh.msc.orders.write.OrderEntity._
import pl.edu.agh.msc.products.ProductService
import pl.edu.agh.msc.utils.Time

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

  private case class InitialOrder(order: Order)

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
      val persistentSender = sender()
      createInitialOrder(draft, user) pipeTo self
      context.become {
        case InitialOrder(order) =>
          persist(OrderCreated(order)){ e =>
            applyEvent(e)
            cartService.clear(user)
            persistentSender ! Ack
          }
          unstashAll()
          context.unbecome()
        case _ => stash()
      }
    case ConfirmOrder() =>
      val persistentSender = sender()
      persist(OrderConfirmed(id)) { e =>
        println("actually confirmed order")
        applyEvent(e)
        persistentSender ! Ack
      }
    case PayOrder() =>
      val persistentSender = sender()
      persist(OrderPaid(id)) { e =>
        println("actually paid order")
        applyEvent(e)
        persistentSender ! Ack
      }
  }

  override def receiveRecover: Receive = {
    case event: Event =>
      println(s"recovering $event")
      applyEvent(event)
  }

  private val applyEvent: Event => Unit = { e =>
    println(s"applying $e")
    val f: Event => Unit = {
      case OrderCreated(order) => state = State.create(order)
      case OrderConfirmed(_)   => state = state.withStatus(OrderStatus.Confirmed)
      case OrderPaid(_)        => state = state.withStatus(OrderStatus.Paid)
    }
    f(e)
  }

  private def createInitialOrder(draft: OrderDraft, user: UUID): Future[InitialOrder] = {
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
      println("actually created inital order")
      InitialOrder(order)
    }
  }
}
