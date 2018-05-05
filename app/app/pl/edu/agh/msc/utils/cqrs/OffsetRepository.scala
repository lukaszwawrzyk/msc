package pl.edu.agh.msc.utils.cqrs

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
    value: Long
  )

  private class Offsets(tag: Tag) extends Table[OffsetRow](tag, "offsets") {
    def name = column[String]("name", O.PrimaryKey)
    def value = column[Long]("value")
    def * = (name, value).mapTo[OffsetRow]
  }

  private val Initial = 0L

  private val baseQuery = TableQuery[Offsets]

  private val byNameQuery = Compiled { name: Rep[String] =>
    baseQuery.filter(_.name === name)
  }
  private val valueByNameQuery = Compiled { name: Rep[String] =>
    baseQuery.filter(_.name === name).map(_.value)
  }

  def getOrCreate(name: String)(implicit ec: ExecutionContext): Future[Long] = db.run {
    byNameQuery(name).result.headOption.flatMap[Long, NoStream, Nothing] {
      case Some(offset) =>
        DBIO.successful(offset.value)
      case None =>
        (baseQuery += OffsetRow(name, Initial)).map(_ => Initial)
    }
  }

  def set(name: String, value: Long)(implicit ec: ExecutionContext): Future[Unit] = db.run {
    valueByNameQuery(name).update(value).map(_ => ())
  }

}
