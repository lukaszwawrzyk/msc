package pl.edu.agh.msc.products

import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.pricing.Money
import pl.edu.agh.msc.products.Filtering.PriceRange
import pl.edu.agh.msc.review.Rating
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }

case class ProductRepoView(
  name:                String,
  cachedPrice:         Money,
  photo:               Option[String],
  cachedAverageRating: Option[Rating],
  description:         String
)

@Singleton class ProductRepository @Inject() (dbConfigProvider: DatabaseConfigProvider) {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  private case class ProductRow(
    name:                String,
    cachedPrice:         BigDecimal,
    photo:               Option[String],
    cachedAverageRating: Option[Double],
    description:         String,
    id:                  Long           = -1
  )

  private class Products(tag: Tag) extends Table[ProductRow](tag, "products") {
    def name = column[String]("name")
    def cachedPrice = column[BigDecimal]("cached_price")
    def photo = column[Option[String]]("photo")
    def cachedAverageRating = column[Option[Double]]("cached_average_rating")
    def description = column[String]("description")
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def * = (name, cachedPrice, photo, cachedAverageRating, description, id).mapTo[ProductRow]
  }

  private val baseQuery = TableQuery[Products]
  private val insertQuery = baseQuery returning baseQuery.map(_.id)
  private val byIdQuery = Compiled { id: Rep[Long] =>
    baseQuery.filter(_.id === id)
  }

  def find(id: ProductId)(implicit ec: ExecutionContext): Future[ProductRepoView] = db.run {
    byIdQuery(id.value).result.head.map { row =>
      ProductRepoView(row.name, Money(row.cachedPrice), row.photo.map(new String(_)), row.cachedAverageRating.map(Rating(_)), row.description)
    }
  }

  def list(
    filtering:  Filtering,
    pagination: Pagination,
    sorting:    Sorting
  )(implicit ec: ExecutionContext): Future[Paginated[ProductShort]] = db.run {
    val filteredQuery = baseQuery.filter { p =>
      filtering.minRating.map(minRating => p.cachedAverageRating >= minRating.value).getOrElse(LiteralColumn(true).?) &&
        filtering.text.map(_.toLowerCase).map(text => p.name.toLowerCase like s"%$text%").getOrElse(LiteralColumn(true)) &&
        filtering.priceRange.map {
          case PriceRange(from, to) =>
            p.cachedPrice >= from.map(_.value) && p.cachedPrice <= to.map(_.value)
        }.map(_.getOrElse(true)).getOrElse(LiteralColumn(true))
    }

    val sortedQuery = if (sorting.byNameAsc) filteredQuery.sortBy(p => p.name.asc) else filteredQuery
    val finalQuery = sortedQuery
      .drop((pagination.page - 1) * pagination.size).take(pagination.size)

    for {
      rows <- finalQuery.result
      count <- filteredQuery.size.result
    } yield {
      val products = rows.map(toListView)
      val totalPages = count / pagination.size + (if (count % pagination.size == 0) 0 else 1)
      Paginated(pagination, totalPages, products)
    }
  }

  private def toListView(row: ProductRow) = {
    ProductShort(row.name, Money(row.cachedPrice), row.photo, row.cachedAverageRating.map(Rating(_)), ProductId(row.id.value))
  }

  def update(id: ProductId, product: ProductRepoView)(implicit ec: ExecutionContext): Future[Unit] = db.run {
    import product._
    DBIO.seq(byIdQuery(id.value).update(ProductRow(name, cachedPrice.value, photo.map(_.toString), cachedAverageRating.map(_.value), description, id.value)))
  }

  def insert(product: ProductRepoView)(implicit ec: ExecutionContext): Future[ProductId] = db.run {
    import product._
    val insert = insertQuery += ProductRow(name, cachedPrice.value, photo.map(_.toString), cachedAverageRating.map(_.value), description)
    insert.map(id => ProductId(id.value))
  }

}
