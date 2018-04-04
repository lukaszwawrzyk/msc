package pl.edu.agh.msc.utils

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.pattern._
import akka.util.Timeout
import javax.inject.Inject

import scala.concurrent.{ Future, TimeoutException }
import scala.concurrent.duration._
import scala.reflect.ClassTag

class ServiceUnavailableException extends RuntimeException

object Guard {
  case class Task(run: () => Future[Any])
}

class Guard extends Actor with ActorLogging {

  import context.dispatcher

  private val breaker =
    new CircuitBreaker(
      context.system.scheduler,
      maxFailures  = 5,
      callTimeout  = 500.millis,
      resetTimeout = 5.seconds
    )

  def receive: Receive = {
    case Guard.Task(action) ⇒
      breaker.withCircuitBreaker(action()).recoverWith {
        case _: CircuitBreakerOpenException | _: TimeoutException =>
          Future.failed(new ServiceUnavailableException)
      } pipeTo sender()
  }

}

class GuardedCall @Inject() (actorSystem: ActorSystem) {

  private val guard = actorSystem.actorOf(Props(new Guard))
  implicit val timeout = Timeout(5.seconds)

  def run[A: ClassTag](f: => Future[A]): Future[A] = {
    (guard ? Guard.Task(() => f)).mapTo[A]
  }

  object implicits {
    implicit class GuardedOps[A: ClassTag](f: => Future[A]) {
      def guarded: Future[A] = run(f)
    }
  }

}