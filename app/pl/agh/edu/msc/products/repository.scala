package pl.agh.edu.msc.products

import javax.inject.{ Inject, Singleton }

import pl.agh.edu.msc.common.infra.Id
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class ProductRepository @Inject()(dbConfigProvider: DatabaseConfigProvider) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  private case class ProductRow(
    name:                String,
    cachedPrice:         BigDecimal,
    photo:               Option[String],
    cachedAverageRating: Option[Double],
    description:         String,
    id:                  Id[ProductRow] = Id(-1)
  )

  private class Products(tag: Tag) extends Table[ProductRow](tag, "products") {
    def name = column[String]("name")
    def cachedPrice = column[BigDecimal]("cached_price")
    def photo = column[Option[String]]("photo")
    def cachedAverageRating = column[Option[Double]]("cached_average_rating")
    def description = column[String]("description")
    def id = column[Id[ProductRow]]("id", O.PrimaryKey, O.AutoInc)
    def * = (name, cachedPrice, photo, cachedAverageRating, description, id).mapTo[ProductRow]
  }

  private val query = TableQuery[Products]

}
