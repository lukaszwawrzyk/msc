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

  def saveDraft(orderDraft: OrderDraft)(implicit ec: ExecutionContext): Future[OrderId] = {
    ???
  }

  def find(id: OrderId)(implicit ec: ExecutionContext): Future[Order] = {
    ???
  }

  def confirm(id: OrderId)(implicit ec: ExecutionContext): Future[Unit] = {
    ???
  }

  def historical(user: UUID)(implicit ec: ExecutionContext): Future[Seq[Order]] = {
    ???
  }

  def paymentConfirmed(id: OrderId)(implicit ec: ExecutionContext): Future[Unit] = {
    ???
  }

}
