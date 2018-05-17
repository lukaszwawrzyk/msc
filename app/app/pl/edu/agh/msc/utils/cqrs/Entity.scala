package pl.edu.agh.msc.utils.cqrs

import akka.pattern._
import akka.persistence.PersistentActor
import akka.persistence.journal.Tagged

import scala.concurrent.Future
import scala.reflect.ClassTag

trait EntityCompanion {
  type Event
  type Command
  implicit val eventClass: ClassTag[Event]
  implicit val commandClass: ClassTag[Command]
  def name: String
  def idExtractor: Command => String
}

object Entity {
  case object Ack
  private case class DelayedResult(value: Any)
}

abstract class Entity[Command: ClassTag, Event: ClassTag] extends PersistentActor {

  import context.dispatcher

  def persistenceId: String = self.path.parent.name + "-" + self.path.name

  override def receiveCommand: Receive = {
    case c: Command => handleCommand(c)
  }

  def handleCommand: Command => Unit

  protected def handlePure(event: Event): Unit = {
    handleEffect(event)(())
  }

  protected def handleEffect(event: Event)(sideEffect: => Unit): Unit = {
    val persistentSender = sender()
    emit(event) { e =>
      applyEvent(e)
      sideEffect
      persistentSender ! Entity.Ack
    }
  }

  protected def handleDelayed[A: ClassTag](initialLogic: Future[A])(event: A => Event)(sideEffect: => Unit)(response: A => Any): Unit = {
    val persistentSender = sender()
    initialLogic.map(Entity.DelayedResult) pipeTo self
    context.become({
      case Entity.DelayedResult(res: A) =>
        emit(event(res)) { e =>
          applyEvent(e)
          sideEffect
          persistentSender ! response(res)
        }
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

  @inline private def emit[A](event: A)(handler: A => Unit): Unit = {
    persist(event)(handler)
  }
}
