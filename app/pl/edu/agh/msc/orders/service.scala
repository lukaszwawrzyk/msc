package pl.edu.agh.msc.orders

import java.util.UUID
import javax.inject.{ Inject, Singleton }

import pl.edu.agh.msc.products.ProductService
import pl.edu.agh.msc.utils.Time

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class OrdersService @Inject() (
  ordersRepository: OrdersRepository,
  productService:   ProductService,
  time:             Time
) {

  def saveDraft(orderDraft: OrderDraft, user: UUID)(implicit ec: ExecutionContext): Future[OrderId] = {
    for {
      items <- Future.traverse(orderDraft.cart.items) { cartItem =>
        productService.price(cartItem.product).map { price =>
          LineItem(cartItem.product, cartItem.amount, price)
        }
      }
      id = OrderId(UUID.randomUUID())
      order = Order(
        id,
        buyer   = user,
        address = orderDraft.address,
        status  = OrderStatus.Unconfirmed,
        items,
        date = time.now()
      )
      _ <- ordersRepository.insert(order)
    } yield id
  }

  def find(id: OrderId)(implicit ec: ExecutionContext): Future[Order] = {
    ordersRepository.find(id)
  }

  def confirm(id: OrderId)(implicit ec: ExecutionContext): Future[Unit] = {
    ordersRepository.changeStatus(id, OrderStatus.Confirmed)
  }

  def historical(user: UUID)(implicit ec: ExecutionContext): Future[Seq[Order]] = {
    ordersRepository.findByUser(user)
  }

  def paymentConfirmed(id: OrderId)(implicit ec: ExecutionContext): Future[Unit] = {
    ???
  }

}
