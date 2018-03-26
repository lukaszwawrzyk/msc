package pl.edu.agh.msc.availability

import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.products.ProductId
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import pl.edu.agh.msc.utils._

@Singleton class AvailabilityRepository @Inject() (dbConfigProvider: DatabaseConfigProvider) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

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

  def find(product: ProductId): Option[Availability] = {
    db.run(byProductQuery(product.value).result.headOption).await().map(row => Availability(row.stock))
  }

  def save(product: ProductId, availability: Availability): Unit = db.run {
    DBIO.seq(
      byProductQuery(product.value).delete,
      baseQuery += AvailabilityRow(product.value, availability.stock)
    ).transactionally
  }.await()

}
