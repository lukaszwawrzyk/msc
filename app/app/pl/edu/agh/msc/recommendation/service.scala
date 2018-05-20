package pl.edu.agh.msc.recommendation

import java.util.UUID

import cats.syntax.option._
import com.google.inject.{ Inject, Singleton }
import pl.edu.agh.msc.products._
import pl.edu.agh.msc.review.Rating
import pl.edu.agh.msc.utils.Cache

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Random

@Singleton class RecommendationService @Inject() (
  productService: ProductService,
  cache:          Cache
) {

  private val MaxWords = 4
  private val MaxRecommendations = 5
  private val MinRating = Rating(4)

  private implicit val ratingOrdering: Ordering[Rating] = Ordering.by(_.value)

  def default()(implicit ec: ExecutionContext): Future[Seq[ProductShort]] = {
    cache.cached(namespace  = "recomm-default", expiration = 5.minutes)("")(_ => findDefaultProducts.map(_.data))
  }

  def forProduct(productId: ProductId, max: Int)(implicit ec: ExecutionContext): Future[Seq[ProductShort]] = {
    cache.cached(namespace  = "recomm-prod", expiration = 1.day)((productId, max))((forProductNonCached _).tupled)
  }

  private def forProductNonCached(productId: ProductId, max: Int)(implicit ec: ExecutionContext): Future[Seq[ProductShort]] = {
    for {
      product <- productService.findShort(productId)
      words = wordsInName(product).take(MaxWords)
      recommendations <- forWords(words, excluding = Set(productId), max)
    } yield recommendations
  }

  def forUser(userId: UUID)(implicit ec: ExecutionContext): Future[Seq[ProductShort]] = {
    default()
  }

  private def forWords(words: Seq[String], excluding: Set[ProductId], max: Int = MaxRecommendations)(implicit ec: ExecutionContext): Future[Seq[ProductShort]] = {
    for {
      recommended <- Future.traverse(words)(findSimilarProducts(_, max))
      backup <- findDefaultProducts
    } yield {
      (recommended.flatMap(_.data).sortBy(_.averageRating).reverse ++ backup.data)
        .distinct
        .filterNot(p => excluding.contains(p.id))
        .take(max)
    }
  }

  private def findSimilarProducts(word: String, max: Int)(implicit ec: ExecutionContext): Future[Paginated[ProductShort]] = {
    val filtering = Filtering(text      = word.some, minRating = MinRating.some)
    productService.list(filtering, firstPage(max))
  }

  private def findDefaultProducts(implicit ec: ExecutionContext) = {
    productService.list(Filtering(minRating = MinRating.some), firstPage(MaxRecommendations))
  }

  private def wordsInName(product: ProductShort) = {
    product.name.split(" ").toSeq
  }

  private def firstPage(size: Int) = Pagination(size = size, page = 1)

}
