package pl.agh.edu.msc.common

import org.scalatest._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.reflect.ClassTag

trait IntegrationTest extends FlatSpecLike with Matchers with GuiceOneAppPerSuite {
  protected def inject[A: ClassTag]: A = app.injector.instanceOf[A]

  protected implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  protected implicit class AwaitOps[A](val f: Future[A]) {
    def await(): A = Await.result(f, Duration.Inf)
  }

}
