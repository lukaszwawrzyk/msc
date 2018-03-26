package pl.edu.agh.msc.orders

import java.util.UUID
import javax.inject.{ Inject, Singleton }

import pl.edu.agh.msc.cart.CartService
import pl.edu.agh.msc.products.ProductService
import pl.edu.agh.msc.utils.Time

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class OrdersService @Inject() (
  ordersRepository: OrdersRepository,
  productService:   ProductService,
  cartService:      CartService,
  time:             Time
) {

  def saveDraft(orderDraft: OrderDraft, user: UUID): OrderId = {
    val items = orderDraft.cart.items.map { cartItem =>
      val price = productService.price(cartItem.product)
      LineItem(cartItem.product, cartItem.amount, price)
    }
    val id = OrderId(UUID.randomUUID())
    val order = Order(
      id,
      buyer   = user,
      address = orderDraft.address,
      status  = OrderStatus.Unconfirmed,
      items,
      date = time.now()
    )
    ordersRepository.insert(order)
    cartService.clear(user)
    id
  }

  def find(id: OrderId): Order = {
    ordersRepository.find(id)
  }

  def confirm(id: OrderId): Unit = {
    ordersRepository.changeStatus(id, OrderStatus.Confirmed)
  }

  def historical(user: UUID): Seq[Order] = {
    ordersRepository.findByUser(user)
  }

  def paymentConfirmed(id: OrderId): Unit = {
    ordersRepository.changeStatus(id, OrderStatus.Paid)
  }

}
