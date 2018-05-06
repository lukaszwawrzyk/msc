package pl.edu.agh.msc

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

}
