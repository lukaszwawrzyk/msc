package pl.edu.agh.msc.common

import org.scalatest._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.DBApi
import play.api.db.evolutions.Evolutions
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.reflect.ClassTag

sealed trait DatabasePreparation extends GuiceOneAppPerSuite with Awaiting with BeforeAndAfter { this: TestSuite =>

  before {
    prepareDbForTest()
  }

  private def prepareDbForTest() {
    cleanDb()
    applyEvolutions()
  }

  private def applyEvolutions() {
    Evolutions.applyEvolutions(app.injector.instanceOf[DBApi].database("default"))
  }

  private def cleanDb() = {
    val dbConfig = app.injector.instanceOf[DatabaseConfigProvider].get[JdbcProfile]
    import dbConfig._
    import dbConfig.profile.api._
    db.run(sql"DROP ALL OBJECTS".as[Int]).await()
  }

}

trait Awaiting {

  protected implicit class AwaitOps[A](val f: Future[A]) {
    def await(): A = Await.result(f, Duration.Inf)
  }

}

trait IntegrationTest extends FlatSpecLike with Matchers with DatabasePreparation {

  protected def inject[A: ClassTag]: A = app.injector.instanceOf[A]

  protected def behaviorOf[A: ClassTag]: A = {
    behavior of implicitly[ClassTag[A]].runtimeClass.getSimpleName
    inject[A]
  }

  protected def withHelpOf[A: ClassTag]: A = inject[A]

  protected implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

}
