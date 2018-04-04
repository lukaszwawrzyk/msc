package pl.edu.agh.msc.availability

import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.products.ProductId
import pl.edu.agh.msc.utils.GuardedCall
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class AvailabilityRepository @Inject() (dbConfigProvider: DatabaseConfigProvider, guardedCall: GuardedCall) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._
  import guardedCall.implicits._

  private case class AvailabilityRow(
    productId: Long,
    stock:     Long,
    id:        Long = -1
  )

  private class AvailabilityTable(tag: Tag) extends Table[AvailabilityRow](tag, "availability") {
    def productId = column[Long]("product_id")
    def stock = column[Long]("stock")
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def * = (productId, stock, id).mapTo[AvailabilityRow]
  }

  private val baseQuery = TableQuery[AvailabilityTable]

  private val byProductQuery = Compiled { productId: Rep[Long] =>
    baseQuery.filter(_.productId === productId)
  }

  def find(product: ProductId)(implicit ec: ExecutionContext): Future[Option[Availability]] = db.run {
    byProductQuery(product.value).result.headOption.map(row => row.map(row => Availability(row.stock)))
  }.guarded

  def save(product: ProductId, availability: Availability)(implicit ec: ExecutionContext): Future[Unit] = db.run {
    DBIO.seq(
      byProductQuery(product.value).delete,
      baseQuery += AvailabilityRow(product.value, availability.stock)
    ).transactionally
  }.guarded

}
