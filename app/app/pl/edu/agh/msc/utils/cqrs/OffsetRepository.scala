package pl.edu.agh.msc.utils.cqrs

import java.util.UUID

import akka.persistence.query.TimeBasedUUID
import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.utils.SlickTypeMappings
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class OffsetRepository @Inject() (dbConfigProvider: DatabaseConfigProvider) extends SlickTypeMappings {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  protected val profile = dbConfig.profile
  import dbConfig.db
  import profile.api._

  private case class OffsetRow(
    name:  String,
    value: Option[UUID]
  )

  private class Offsets(tag: Tag) extends Table[OffsetRow](tag, "offsets") {
    def name = column[String]("name", O.PrimaryKey)
    def value = column[Option[UUID]]("value")
    def * = (name, value).mapTo[OffsetRow]
  }

  private val baseQuery = TableQuery[Offsets]

  private val byNameQuery = Compiled { name: Rep[String] =>
    baseQuery.filter(_.name === name)
  }
  private val valueByNameQuery = Compiled { name: Rep[String] =>
    baseQuery.filter(_.name === name).map(_.value)
  }

  def getOrCreate(name: String)(implicit ec: ExecutionContext): Future[Option[TimeBasedUUID]] = db.run {
    byNameQuery(name).result.headOption.flatMap[Option[TimeBasedUUID], NoStream, Nothing] {
      case Some(offset) =>
        DBIO.successful(offset.value.map(TimeBasedUUID))
      case None =>
        (baseQuery += OffsetRow(name, None)).map(_ => None)
    }
  }

  def set(name: String, offset: TimeBasedUUID)(implicit ec: ExecutionContext): Future[Unit] = db.run {
    valueByNameQuery(name).update(Some(offset.value)).map(_ => ())
  }

}
