package pl.edu.agh.msc.orders.write

import akka.actor.{ ActorSystem, Props }
import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.cart.CartService
import pl.edu.agh.msc.orders.OrderId
import pl.edu.agh.msc.products.ProductService
import pl.edu.agh.msc.utils.Time
import pl.edu.agh.msc.utils.cqrs.EntitiesFacade

@Singleton class OrderEntitiesFacade @Inject() (
  productService:  ProductService,
  cartService:     CartService,
  time:            Time,
  val actorSystem: ActorSystem
) extends EntitiesFacade[OrderId] {

  override protected def props(id: OrderId): Props = {
    Props(new OrderEntity(id, productService, cartService, time))
  }

}
