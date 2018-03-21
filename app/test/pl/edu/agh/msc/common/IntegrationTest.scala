package pl.edu.agh.msc.common

import com.google.inject.Module
import org.scalatest._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import pl.edu.agh.msc.InitModule
import play.api.{ Application, Configuration, Environment }
import play.api.db.DBApi
import play.api.db.evolutions.Evolutions
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.{ BinderOption, GuiceApplicationBuilder, GuiceableModule }
import slick.jdbc.JdbcProfile

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.reflect.ClassTag

sealed trait DatabasePreparation extends GuiceOneAppPerSuite with Awaiting with BeforeAndAfter { this: TestSuite =>

  protected def configureApp(app: GuiceApplicationBuilder): GuiceApplicationBuilder = app

  override def fakeApplication(): Application = {
    val baseConfig = new GuiceApplicationBuilder()
      .configure("slick.dbs.default.db.url" -> "jdbc:postgresql://localhost:5432/msc-test")
      .disable[InitModule]
    configureApp(baseConfig).build()
  }

  before {
    prepareDbForTest()
  }

  private def prepareDbForTest() {
    cleanDb()
    applyEvolutions()
  }

  private def cleanDb() = {
    val dbConfig = app.injector.instanceOf[DatabaseConfigProvider].get[JdbcProfile]
    import dbConfig._
    import dbConfig.profile.api._
    db.run(sql"drop schema public cascade".as[Int]).await()
    db.run(sql"create schema public".as[Int]).await()
  }

  private def applyEvolutions() {
    Evolutions.applyEvolutions(app.injector.instanceOf[DBApi].database("default"))
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
