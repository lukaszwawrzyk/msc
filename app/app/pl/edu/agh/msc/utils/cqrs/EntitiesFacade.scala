package pl.edu.agh.msc.utils.cqrs

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.pattern.{ ask => akkaAsk }
import akka.util.Timeout

import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.reflect.ClassTag

trait EntitiesFacade[Id] {

  protected def actorSystem: ActorSystem

  private implicit lazy val ec: ExecutionContext = actorSystem.dispatcher

  private val entities = new java.util.concurrent.ConcurrentHashMap[Id, ActorRef].asScala

  def call(id: Id, message: Any): Future[Unit] = {
    ask[Entity.Ack.type](id, message).map(_ => ())
  }

  def ask[A: ClassTag](id: Id, message: Any): Future[A] = {
    val actorRef = entities.getOrElseUpdate(id, create(id))
    ask(message, actorRef).mapTo[A]
  }

  private def ask[A](message: Any, actorRef: ActorRef) = {
    (actorRef ? message)(Timeout(2.seconds))
  }

  private def create(id: Id) = {
    actorSystem.actorOf(props(id))
  }

  protected def props(id: Id): Props
}
