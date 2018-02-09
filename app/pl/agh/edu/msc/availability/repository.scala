package pl.agh.edu.msc.availability

import javax.inject.{ Inject, Singleton }

import pl.agh.edu.msc.common.infra.Id
import pl.agh.edu.msc.products.ProductId
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class AvailabilityRepository @Inject()(dbConfigProvider: DatabaseConfigProvider) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  private case class AvailabilityRow(
    productId: Long,
    stock:     Long,
    id:        Id[AvailabilityRow] = Id(-1)
  )

  private class AvailabilityTable(tag: Tag) extends Table[AvailabilityRow](tag, "availability") {
    def productId = column[Long]("product_id")
    def stock = column[Long]("stock")
    def id = column[Id[AvailabilityRow]]("id", O.PrimaryKey, O.AutoInc)
    def * = (productId, stock, id).mapTo[AvailabilityRow]
  }

  private val baseQuery = TableQuery[AvailabilityTable]

  private val byIdQuery = Compiled { productId: Rep[Long] =>
    baseQuery.filter(_.productId === productId)
  }

  def find(product: ProductId)(implicit ec: ExecutionContext): Future[Option[Availability]] = db.run {
    byIdQuery(product.value).result.headOption.map(row => row.map(row => Availability(row.stock)))
  }

  def save(product: ProductId, availability: Availability)(implicit ec: ExecutionContext): Future[Unit] = db.run {
    byIdQuery(product.value).delete >> (baseQuery += AvailabilityRow(product.value, availability.stock)) >> DBIO.successful(())
  }

}
