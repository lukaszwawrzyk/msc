package pl.edu.agh.msc

import scala.concurrent.{ ExecutionContext, Future }

package object utils {

  def buildMap[K, V](keys: Seq[K])(getValue: K => Future[V])(implicit ec: ExecutionContext): Future[Map[K, V]] = {
    Future.traverse(keys.distinct)(key => getValue(key).map(key -> _)).map(_.toMap)
  }

}
