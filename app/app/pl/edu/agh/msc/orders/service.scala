package pl.edu.agh.msc.orders

import java.util.UUID

import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.orders.read.{ OrdersEventMapper, OrdersRepository }
import pl.edu.agh.msc.orders.write.{ OrderEntity, OrderEntityFacade }

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class OrdersService @Inject() (
  ordersRepository:  OrdersRepository,
  orderEntityFacade: OrderEntityFacade,
  ordersEventMapper: OrdersEventMapper
) {

  private type Ack = OrderEntity.Ack.type

  ordersEventMapper.run()

  def find(id: OrderId)(implicit ec: ExecutionContext): Future[Order] = {
    ordersRepository.find(id)
  }

  def historical(user: UUID)(implicit ec: ExecutionContext): Future[Seq[Order]] = {
    ordersRepository.findByUser(user)
  }

  def saveDraft(orderDraft: OrderDraft, user: UUID)(implicit ec: ExecutionContext): Future[OrderId] = {
    val id = OrderId(UUID.randomUUID())
    orderEntityFacade.ask[Ack](id, OrderEntity.CreateOrder(orderDraft, user)).map(_ => id)
  }

  def confirm(id: OrderId)(implicit ec: ExecutionContext): Future[Unit] = {
    orderEntityFacade.ask[Ack](id, OrderEntity.ConfirmOrder()).map(_ => ())
  }

  def paymentConfirmed(id: OrderId)(implicit ec: ExecutionContext): Future[Unit] = {
    orderEntityFacade.ask[Ack](id, OrderEntity.PayOrder()).map(_ => ())
  }

}
