package pl.edu.agh.msc.orders.read

import akka.actor.ActorSystem
import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.orders.OrderStatus
import pl.edu.agh.msc.orders.write.OrderEntity
import pl.edu.agh.msc.utils.cqrs.{ EventMapper, OffsetRepository }

@Singleton class OrdersEventMapper @Inject() (
  system:           ActorSystem,
  offsetRepository: OffsetRepository,
  ordersRepository: OrdersRepository
) extends EventMapper(OrderEntity, system, offsetRepository) {

  import system.dispatcher

  override protected val process = {
    case OrderEntity.OrderCreated(order) => ordersRepository.insert(order)
    case OrderEntity.OrderConfirmed(id)  => ordersRepository.changeStatus(id, OrderStatus.Confirmed)
    case OrderEntity.OrderPaid(id)       => ordersRepository.changeStatus(id, OrderStatus.Paid)
  }

}
