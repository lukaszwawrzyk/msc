package pl.edu.agh.msc.orders.read

import akka.actor.ActorSystem
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.persistence.query.{ PersistenceQuery, Sequence }
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
  private val readJournal = PersistenceQuery(system).readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)

  import system.dispatcher

  def run(): Unit = {
    offsetRepository.getOrCreate(entityTag).foreach { initialOffset =>
      implicit val mat: Materializer = ActorMaterializer()(system)
      readJournal
        .eventsByTag(entityTag, Sequence(initialOffset))
        .mapAsync(1) { envelope =>
          process(envelope.event.asInstanceOf[OrderEntity.Event]).map(_ => envelope.offset)
        }.mapAsync(1) {
          case Sequence(offset) =>
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
