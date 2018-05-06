package pl.edu.agh.msc.orders

import java.util.UUID

import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.orders.read.{ OrdersEventMapper, OrdersRepository }
import pl.edu.agh.msc.orders.write.{ OrderEntitiesFacade, OrderEntity }

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class OrdersService @Inject() (
  ordersRepository:  OrdersRepository,
  entitiesFacade:    OrderEntitiesFacade,
  ordersEventMapper: OrdersEventMapper
) {

  ordersEventMapper.run()

  def find(id: OrderId)(implicit ec: ExecutionContext): Future[Order] = {
    ordersRepository.find(id)
  }

  def historical(user: UUID)(implicit ec: ExecutionContext): Future[Seq[Order]] = {
    ordersRepository.findByUser(user)
  }

  def saveDraft(orderDraft: OrderDraft, user: UUID)(implicit ec: ExecutionContext): Future[Order] = {
    val id = OrderId(UUID.randomUUID())
    entitiesFacade.ask[Order](id, OrderEntity.CreateOrder(orderDraft, user))
  }

  def confirm(id: OrderId)(implicit ec: ExecutionContext): Future[Unit] = {
    entitiesFacade.call(id, OrderEntity.ConfirmOrder())
  }

  def paymentConfirmed(id: OrderId)(implicit ec: ExecutionContext): Future[Unit] = {
    entitiesFacade.call(id, OrderEntity.PayOrder())
  }

}