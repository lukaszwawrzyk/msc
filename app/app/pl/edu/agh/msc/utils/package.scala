package pl.edu.agh.msc

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

package object utils {

  def buildMap[K, V](keys: Seq[K])(getValue: K => V): Map[K, V] = {
    keys.distinct.map(key => key -> getValue(key)).toMap
  }

  implicit class BlockingOps[A](val a: Future[A]) {
    def await(): A = Await.result(a, 5.minutes)
  }

}
