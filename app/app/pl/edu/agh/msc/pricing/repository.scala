package pl.edu.agh.msc.pricing

import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.products._
import pl.edu.agh.msc.utils.GuardedCall
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class PriceRepository @Inject() (dbConfigProvider: DatabaseConfigProvider, guardedCall: GuardedCall) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._
  import guardedCall.implicits._

  private case class PriceRow(
    productId: Long,
    price:     BigDecimal,
    id:        Long       = -1
  )

  private class Prices(tag: Tag) extends Table[PriceRow](tag, "prices") {
    def productId = column[Long]("product_id")
    def price = column[BigDecimal]("price")
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def * = (productId, price, id).mapTo[PriceRow]
  }

  private val baseQuery = TableQuery[Prices]

  private val byProductQuery = Compiled { productId: Rep[Long] =>
    baseQuery.filter(_.productId === productId)
  }

  def find(product: ProductId)(implicit ec: ExecutionContext): Future[Option[Money]] = db.run {
    byProductQuery(product.value).result.headOption.map(_.map(row => Money(row.price)))
  }.guarded

  def save(product: ProductId, price: Money)(implicit ec: ExecutionContext): Future[Unit] = db.run {
    DBIO.seq(
      byProductQuery(product.value).delete,
      baseQuery += PriceRow(product.value, price.value)
    ).transactionally
  }.guarded

}
