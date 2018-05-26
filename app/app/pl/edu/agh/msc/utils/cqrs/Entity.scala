package pl.edu.agh.msc.utils.cqrs

import akka.actor.ActorRef
import akka.pattern._
import akka.persistence.PersistentActor
import akka.persistence.journal.Tagged

import scala.concurrent.Future
import scala.reflect.ClassTag

trait EntityCompanion {
  type Event
  type Command
  type Query
  implicit val eventClass: ClassTag[Event]
  implicit val commandClass: ClassTag[Command]
  implicit val queryClass: ClassTag[Query]
  def name: String
  def commandIdExtractor: Command => String
  def queryIdExtractor: Query => String
}

object Entity {
  case object Ack
  private case class DelayedResult(value: Any)
}

abstract class Entity[Command: ClassTag, Event: ClassTag, Query: ClassTag] extends PersistentActor {

  import context.dispatcher

  def persistenceId: String = self.path.parent.name + "-" + self.path.name

  override def receiveCommand: Receive = {
    case c: Command => handleCommand(c)
    case q: Query   => sender() ! handleQuery(q)
  }

  def handleCommand: Command => Unit
  def handleQuery: Query => Any

  protected def handlePure(event: Event): Unit = {
    handleEffect(event)(())
  }

  protected def handleEffect(event: Event)(sideEffect: => Unit): Unit = {
    val persistentSender = sender()
    handleEffect(persistentSender)(event)(sideEffect)
  }

  protected def handleDelayed(createEvent: Future[Event])(sideEffect: => Unit): Unit = {
    val persistentSender = sender()
    createEvent.map(Entity.DelayedResult) pipeTo self
    context.become({
      case Entity.DelayedResult(event: Event) =>
        handleEffect(persistentSender)(event)(sideEffect)
        unstashAll()
        context.unbecome()
      case _ => stash()
    }, discardOld = false)
  }

  override def receiveRecover: Receive = {
    case Tagged(event: Event, _) =>
      applyEvent(event)
    case event: Event =>
      applyEvent(event)
  }

  protected def applyEvent: Event => Unit

  private def handleEffect(persistentSender: ActorRef)(event: Event)(sideEffect: => Unit): Unit = {
    emit(event) { e =>
      applyEvent(e)
      sideEffect
      persistentSender ! Entity.Ack
    }
  }

  @inline private def emit[A](event: A)(handler: A => Unit): Unit = {
    persist(event)(handler)
  }
}
