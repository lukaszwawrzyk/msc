package pl.edu.agh.msc.orders

import java.util.UUID

import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.orders.read.{ OrdersEventMapper, OrdersRepository }
import pl.edu.agh.msc.orders.write.{ OrderEntitiesFacade, OrderEntity }

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class OrdersService @Inject() (
  ordersRepository: OrdersRepository,
  entitiesFacade:   OrderEntitiesFacade
) {

  def find(id: OrderId)(implicit ec: ExecutionContext): Future[Order] = {
    entitiesFacade.query[Option[Order]](OrderEntity.GetOrder(id)).map(_.get)
  }

  def historical(user: UUID)(implicit ec: ExecutionContext): Future[Seq[Order]] = {
    ordersRepository.findByUser(user)
  }

  def saveDraft(orderDraft: OrderDraft, user: UUID)(implicit ec: ExecutionContext): Future[OrderId] = {
    val id = OrderId(UUID.randomUUID())
    entitiesFacade.command(OrderEntity.CreateOrder(id, orderDraft, user)).map(_ => id)
  }

  def confirm(id: OrderId)(implicit ec: ExecutionContext): Future[Unit] = {
    entitiesFacade.command(OrderEntity.ConfirmOrder(id))
  }

  def paymentConfirmed(id: OrderId)(implicit ec: ExecutionContext): Future[Unit] = {
    entitiesFacade.command(OrderEntity.PayOrder(id))
  }

}