package pl.edu.agh.msc.pricing

import javax.inject.{ Inject, Singleton }

import pl.edu.agh.msc.common.infra.Id
import pl.edu.agh.msc.products._
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class PriceRepository @Inject() (dbConfigProvider: DatabaseConfigProvider) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  private case class PriceRow(
    productId: Long,
    price:     BigDecimal,
    id:        Id[PriceRow] = Id(-1)
  )

  private class Prices(tag: Tag) extends Table[PriceRow](tag, "prices") {
    def productId = column[Long]("product_id")
    def price = column[BigDecimal]("price")
    def id = column[Id[PriceRow]]("id", O.PrimaryKey, O.AutoInc)
    def * = (productId, price, id).mapTo[PriceRow]
  }

  private val baseQuery = TableQuery[Prices]

  private val byIdQuery = Compiled { productId: Rep[Long] =>
    baseQuery.filter(_.productId === productId)
  }

  def find(product: ProductId)(implicit ec: ExecutionContext): Future[Money] = db.run {
    byIdQuery(product.value).result.head.map(row => Money(row.price))
  }

  def save(product: ProductId, price: Money)(implicit ec: ExecutionContext): Future[Unit] = db.run {
    byIdQuery(product.value).delete >> (baseQuery += PriceRow(product.value, price.value)) >> DBIO.successful(())
  }

}
