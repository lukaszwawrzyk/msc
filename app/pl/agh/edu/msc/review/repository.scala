package pl.agh.edu.msc.review

import javax.inject.{ Inject, Singleton }

import pl.agh.edu.msc.common.infra.Id
import pl.agh.edu.msc.products.ProductId
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class ReviewRepository @Inject() (dbConfigProvider: DatabaseConfigProvider) {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  private case class ReviewRow(
    author:    String,
    content:   String,
    rating:    Double,
    productId: Long,
    id:        Id[ReviewRow] = Id(-1)
  )

  private class Reviews(tag: Tag) extends Table[ReviewRow](tag, "reviews") {
    def author = column[String]("author")
    def content = column[String]("content")
    def rating = column[Double]("rating")
    def productId = column[Long]("product_id")
    def id = column[Id[ReviewRow]]("id", O.PrimaryKey, O.AutoInc)
    def * = (author, content, rating, productId, id).mapTo[ReviewRow]
  }

  private val baseQuery = TableQuery[Reviews]

  private val averageRatingQuery = Compiled { product: Rep[Long] =>
    baseQuery.filter(_.productId === product).map(_.rating).avg
  }

  private val byIdQuery = Compiled { product: Rep[Long] =>
    baseQuery.filter(_.productId === product)
  }

  def averageRating(product: ProductId)(implicit ec: ExecutionContext): Future[Option[Rating]] = db.run {
    averageRatingQuery(product.value).result.map(_.map(Rating(_)))
  }

  def find(product: ProductId)(implicit ec: ExecutionContext): Future[Seq[Review]] = db.run {
    byIdQuery(product.value).result.map { rows =>
      rows.map { row =>
        Review(row.author, row.content, Rating(row.rating))
      }
    }
  }

  def insert(product: ProductId, review: Review)(implicit ec: ExecutionContext): Future[Unit] = db.run {
    import review._
    (baseQuery += ReviewRow(author, content, rating.value, product.value)).map(_ => ())
  }

}
