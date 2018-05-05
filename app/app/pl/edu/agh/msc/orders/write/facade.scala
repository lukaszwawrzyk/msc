package pl.edu.agh.msc.orders.write

import akka.actor.{ ActorRef, ActorSystem, Props }
import pl.edu.agh.msc.cart.CartService
import pl.edu.agh.msc.orders.OrderId
import pl.edu.agh.msc.products.ProductService
import pl.edu.agh.msc.utils.Time
import akka.pattern.{ ask => akkaAsk, _ }

import scala.collection.JavaConverters._
import javax.inject.{ Inject, Singleton }
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.reflect.ClassTag

@Singleton class OrderEntityFacade @Inject() (
  productService: ProductService,
  cartService:    CartService,
  time:           Time,
  actorSystem:    ActorSystem
) {

  private val entities = new java.util.concurrent.ConcurrentHashMap[OrderId, ActorRef].asScala

  def ask[A: ClassTag](id: OrderId, message: Any): Future[A] = {
    val actorRef = entities.getOrElseUpdate(id, create(id))
    ask(message, actorRef).mapTo[A]
  }

  private def ask[A](message: Any, actorRef: ActorRef) = {
    (actorRef ? message)(Timeout(2.seconds))
  }

  private def create(id: OrderId) = {
    actorSystem.actorOf(Props(new OrderEntity(id, productService, cartService, time)))
  }
}
