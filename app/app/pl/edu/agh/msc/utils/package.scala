package pl.edu.agh.msc

import java.util.concurrent.TimeoutException

import akka.actor.ActorSystem

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future, Promise }

package object utils {

  def buildMap[K, V](keys: Seq[K])(getValue: K => Future[V])(implicit ec: ExecutionContext): Future[Map[K, V]] = {
    Future.traverse(keys.distinct)(key => getValue(key).map(key -> _)).map(_.toMap)
  }

  implicit class DelayedFuture(val f: Future.type) {
    def delay(delay: FiniteDuration)(implicit system: ActorSystem, ec: ExecutionContext): Future[Unit] = {
      val promise = Promise[Unit]
      system.scheduler.scheduleOnce(delay)(promise.success(()))
      promise.future
    }
  }

  def waitUntil(condition: => Future[Boolean])(initialDelay: FiniteDuration, delays: FiniteDuration*)(implicit system: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    def recur(delays: List[FiniteDuration]): Future[Unit] = {
      println("checking cond")
      condition.flatMap { suceeded =>
        if (suceeded) {
          println("succeeded")
          Future.successful(())
        } else {
          delays match {
            case d :: ds =>
              println(s"delaying again for $d")
              Future.delay(d).flatMap(_ => recur(ds))
            case Nil => Future.failed(new TimeoutException("Concition not met after all attemps."))
          }
        }
      }
    }

    println(s"started delay $initialDelay")
    Future.delay(initialDelay).flatMap(_ => recur(delays.toList))
  }

}
