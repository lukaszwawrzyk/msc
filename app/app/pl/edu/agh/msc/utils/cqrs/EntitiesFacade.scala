package pl.edu.agh.msc.utils.cqrs

import akka.actor.{ ActorSystem, Props }
import akka.cluster.sharding.ShardRegion.{ ExtractEntityId, ExtractShardId }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import akka.pattern.{ ask => akkaAsk }
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag

abstract class EntitiesFacade(companion: EntityCompanion) {
  import companion.{ commandClass, queryClass }

  private val Shards = 50

  protected def actorSystem: ActorSystem

  private implicit lazy val ec: ExecutionContext = actorSystem.dispatcher
  private val region = ClusterSharding(actorSystem).start(
    typeName        = companion.name,
    entityProps     = props,
    settings        = ClusterShardingSettings(actorSystem),
    extractEntityId = extractId,
    extractShardId  = extractShardId
  )

  private def extractShardId: ExtractShardId = extractId.andThen { case (id, msg) => toShardId(id) }

  private def extractId: ExtractEntityId = {
    case message @ ShardRegion.StartEntity(id) => id -> message
    case message: companion.Command =>
      companion.commandIdExtractor(message) -> message
    case message: companion.Query =>
      companion.queryIdExtractor(message) -> message
  }

  private def toShardId(id: String) = {
    (math.abs(id.hashCode) % Shards).toString
  }

  def command(message: Any): Future[Unit] = {
    ask[Entity.Ack.type](message).map(_ => ())
  }

  def query[A: ClassTag](message: Any): Future[A] = {
    ask[A](message)
  }

  private def ask[A: ClassTag](message: Any): Future[A] = {
    (region ? message)(Timeout(5.seconds)).mapTo[A]
  }

  protected def props: Props
}
