package pl.edu.agh.msc.utils

import cats.instances.future._
import cats.data.OptionT
import javax.inject.Inject
import play.api.cache.AsyncCacheApi

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag

trait KeyFormat[A] {
  def asKey(key: A): String
}

object KeyFormat {
  def apply[A](serialize: A => String): KeyFormat[A] = (key: A) => serialize(key)

  implicit def defaultKeyFormat[A]: KeyFormat[A] = _.toString
}

class Cache @Inject() (cache: AsyncCacheApi) {

  def cached[K: KeyFormat, V: ClassTag](
    namespace:  String,
    expiration: Duration = Duration.Inf
  )(key: K)(fetch: K => Future[V])(implicit ec: ExecutionContext): Future[V] = {
    val formattedKey = asKey(key, namespace)
    cache.getOrElseUpdate(formattedKey, expiration)(fetch(key))
  }

  def cachedOpt[K: KeyFormat, V: ClassTag](
    namespace:  String,
    expiration: Duration = Duration.Inf
  )(key: K)(fetch: K => Future[Option[V]])(implicit ec: ExecutionContext): Future[Option[V]] = {
    val formattedKey = asKey(key, namespace)
    val cachedValue = OptionT(cache.get[V](formattedKey))
    cachedValue.orElse {
      OptionT(fetch(key)).semiflatMap { value =>
        cache.set(formattedKey, value, expiration).map(_ => value)
      }
    }.value
  }

  private def asKey[K: KeyFormat](key: K, namespace: String) = {
    val format = implicitly[KeyFormat[K]]
    sanitize(namespace + "." + format.asKey(key))
  }

  private def sanitize(s: String): String = {
    s.replaceAll("\n|\r|\\s", "")
  }

}
