package pl.edu.agh.msc.review

import java.sql.Timestamp
import java.time.LocalDateTime
import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.products.ProductId
import pl.edu.agh.msc.utils.SlickTypeMappings
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class ReviewRepository @Inject() (dbConfigProvider: DatabaseConfigProvider) extends SlickTypeMappings {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  protected val profile = dbConfig.profile
  import dbConfig.db
  import profile.api._

  private case class ReviewRow(
    author:    String,
    content:   String,
    rating:    Double,
    date:      LocalDateTime,
    productId: Long,
    id:        Long          = -1
  )

  private class Reviews(tag: Tag) extends Table[ReviewRow](tag, "reviews") {
    def author = column[String]("author")
    def content = column[String]("content")
    def rating = column[Double]("rating")
    def date = column[LocalDateTime]("date")
    def productId = column[Long]("product_id")
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def * = (author, content, rating, date, productId, id).mapTo[ReviewRow]
  }

  private val baseQuery = TableQuery[Reviews]

  private val averageRatingQuery = Compiled { product: Rep[Long] =>
    baseQuery.filter(_.productId === product).map(_.rating).avg
  }

  private val byProductQuery = Compiled { product: Rep[Long] =>
    baseQuery.filter(_.productId === product)
  }

  def averageRating(product: ProductId)(implicit ec: ExecutionContext): Future[Option[Rating]] = db.run {
    averageRatingQuery(product.value).result.map(_.map(Rating(_)))
  }

  def find(product: ProductId)(implicit ec: ExecutionContext): Future[Seq[Review]] = db.run {
    byProductQuery(product.value).result.map(convertRows((_, r) => r))
  }

  def latest(limit: Int)(implicit ec: ExecutionContext): Future[Seq[(Review, ProductId)]] = db.run {
    baseQuery.sortBy(_.date.desc).take(limit).result.map(convertRows(((row, r) => r -> ProductId(row.productId))))
  }

  def insert(product: ProductId, review: Review)(implicit ec: ExecutionContext): Future[Unit] = db.run {
    import review._
    (baseQuery += ReviewRow(author, content, rating.value, date, product.value)).map(_ => ())
  }

  private def convertRows[A](transform: (ReviewRow, Review) => A)(rows: Seq[ReviewRow]): Seq[A] = {
    rows.map { row =>
      val review = Review(row.author, row.content, Rating(row.rating), row.date)
      transform(row, review)
    }
  }

}
