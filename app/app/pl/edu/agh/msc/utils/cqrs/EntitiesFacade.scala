package pl.edu.agh.msc.utils.cqrs

import akka.actor.{ ActorSystem, Props }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import akka.pattern.{ ask => akkaAsk }
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag

abstract class EntitiesFacade(companion: EntityCompanion) {
  import companion.commandClass

  private val Shards = 50

  protected def actorSystem: ActorSystem

  private implicit lazy val ec: ExecutionContext = actorSystem.dispatcher
  private val region = ClusterSharding(actorSystem).start(
    typeName        = companion.name,
    entityProps     = props,
    settings        = ClusterShardingSettings(actorSystem),
    extractEntityId = {
      case message @ ShardRegion.StartEntity(id) => id -> message
      case message: companion.Command =>
        companion.idExtractor(message) -> message
    },
    extractShardId  = {
      case ShardRegion.StartEntity(id) => toShardId(id)
      case message: companion.Command =>
        toShardId(companion.idExtractor(message))
    }
  )

  private def toShardId(id: String) = {
    (math.abs(id.hashCode) % Shards).toString
  }

  def call(message: Any): Future[Unit] = {
    ask[Entity.Ack.type](message).map(_ => ())
  }

  def ask[A: ClassTag](message: Any): Future[A] = {
    (region ? message)(Timeout(5.seconds)).mapTo[A]
  }

  protected def props: Props
}
