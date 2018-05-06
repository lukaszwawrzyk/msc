package pl.edu.agh.msc.orders.read

import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{ Offset, PersistenceQuery, Sequence, TimeBasedUUID }
import akka.stream.scaladsl.Sink
import akka.stream.{ ActorMaterializer, Materializer }
import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.orders.OrderStatus
import pl.edu.agh.msc.orders.write.OrderEntity
import pl.edu.agh.msc.utils.cqrs.OffsetRepository

import scala.concurrent.Future

@Singleton class OrdersEventMapper @Inject() (
  system:           ActorSystem,
  offsetRepository: OffsetRepository,
  ordersRepository: OrdersRepository
) {

  private val entityTag = "orders"
  private val readJournal = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  import system.dispatcher

  def run(): Unit = {
    offsetRepository.getOrCreate(entityTag).foreach { initialOffset =>
      implicit val mat: Materializer = ActorMaterializer()(system)
      readJournal
        .eventsByTag(entityTag, initialOffset.getOrElse(Offset.noOffset))
        .mapAsync(1) { envelope =>
          process(envelope.event.asInstanceOf[OrderEntity.Event])
            .recover { case e: Exception => println(s"Exception while applying event: $e") }
            .map(_ => envelope.offset)
        }.mapAsync(1) {
          case offset: TimeBasedUUID =>
            offsetRepository.set(entityTag, offset)
        }.runWith(Sink.ignore)
    }
  }

  private val process: OrderEntity.Event => Future[Unit] = {
    case OrderEntity.OrderCreated(order) => ordersRepository.insert(order)
    case OrderEntity.OrderConfirmed(id)  => ordersRepository.changeStatus(id, OrderStatus.Confirmed)
    case OrderEntity.OrderPaid(id)       => ordersRepository.changeStatus(id, OrderStatus.Paid)
  }

}
