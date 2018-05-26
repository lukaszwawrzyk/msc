package pl.edu.agh.msc.orders.write

import java.util.UUID

import pl.edu.agh.msc.cart.CartService
import pl.edu.agh.msc.orders._
import pl.edu.agh.msc.products.ProductService
import pl.edu.agh.msc.utils.Time
import pl.edu.agh.msc.utils.cqrs.{ Entity, EntityCompanion }
import scala.concurrent.Future
import scala.reflect.ClassTag

object OrderEntity extends EntityCompanion {
  override val eventClass = implicitly
  override val commandClass = implicitly
  override def name = "orders"
  override def idExtractor: Command => String = _.id.value.toString

  sealed trait Event extends Serializable
  final case class OrderCreated(order: Order) extends Event
  final case class OrderConfirmed(id: OrderId) extends Event
  final case class OrderPaid(id: OrderId) extends Event

  sealed trait Command { def id: OrderId }
  final case class CreateOrder(id: OrderId, orderDraft: OrderDraft, user: UUID) extends Command
  final case class ConfirmOrder(id: OrderId) extends Command
  final case class PayOrder(id: OrderId) extends Command

  private case class State(
    order: Option[Order]
  ) {
    def withStatus(status: OrderStatus.Value): State = {
      require(status != order.get.status)
      State(order.map(_.copy(status = status)))
    }
    def create(initialOrder: Order): State = {
      require(order.isEmpty)
      require(initialOrder.status == OrderStatus.Unconfirmed)
      State(Some(initialOrder))
    }
  }

  private object State {
    def notCreated = State(None)
  }

}

import OrderEntity._

class OrderEntity(
  productService: ProductService,
  cartService:    CartService,
  time:           Time
) extends Entity[Command, Event] {

  import context.dispatcher

  private var state = State.notCreated

  override val handleCommand = {
    case CreateOrder(id, draft, user) =>
      handleDelayed(createInitialOrder(id, draft, user))(sideEffect = cartService.clear(user))
    case ConfirmOrder(id) =>
      handlePure(OrderConfirmed(id))
    case PayOrder(id) =>
      handlePure(OrderPaid(id))
  }

  override val applyEvent = {
    case OrderCreated(order) => state = state.create(order)
    case OrderConfirmed(_)   => state = state.withStatus(OrderStatus.Confirmed)
    case OrderPaid(_)        => state = state.withStatus(OrderStatus.Paid)
  }

  private def createInitialOrder(id: OrderId, draft: OrderDraft, user: UUID): Future[OrderCreated] = {
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
    } yield OrderCreated(order)
  }

}