package pl.edu.agh.msc.utils.cqrs

import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{ Offset, PersistenceQuery, TimeBasedUUID }
import akka.stream.scaladsl.Sink
import akka.stream.{ ActorMaterializer, Materializer, ThrottleMode }
import scala.concurrent.duration._

import scala.concurrent.Future

abstract class EventMapper(
  protected val companion: EntityCompanion,
  system:                  ActorSystem,
  offsetRepository:        OffsetRepository
) {

  private val entityTag = companion.name
  private val readJournal = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  import system.dispatcher

  def run(): Unit = {
    offsetRepository.getOrCreate(entityTag).foreach { initialOffset =>
      implicit val mat: Materializer = ActorMaterializer()(system)
      readJournal
        .eventsByTag(entityTag, initialOffset.getOrElse(Offset.noOffset))
        .throttle(200, 1.second, 1, ThrottleMode.shaping)
        .mapAsync(1) { envelope =>
          process(envelope.event.asInstanceOf[companion.Event])
            .recover { case e: Exception => println(s"Exception while applying event: $e in ${companion.name} stream") }
            .map(_ => envelope.offset)
        }.mapAsync(1) {
          case offset: TimeBasedUUID =>
            offsetRepository.set(entityTag, offset)
        }.runWith(Sink.ignore)
    }
  }

  protected def process: companion.Event => Future[Unit]
}