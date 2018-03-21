package pl.edu.agh.msc.recommendation

import java.util.UUID

import cats.syntax.option._
import com.google.inject.{ Inject, Singleton }
import pl.edu.agh.msc.orders.OrdersService
import pl.edu.agh.msc.products._
import pl.edu.agh.msc.review.Rating

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Random

@Singleton class RecommendationService @Inject() (
  productService: ProductService,
  ordersService:  OrdersService
) {

  private val MaxWords = 4
  private val MaxRecommendations = 5
  private val MaxRecentProducts = 5
  private val MinRating = Rating(4)

  private implicit val ratingOrdering: Ordering[Rating] = Ordering.by(_.value)

  def default()(implicit ec: ExecutionContext): Future[Seq[ProductShort]] = {
    findDefaultProducts.map(_.data)
  }

  def forProduct(productId: ProductId)(implicit ec: ExecutionContext): Future[Seq[ProductShort]] = {
    for {
      product <- productService.findDetailed(productId)
      words = wordsInName(product).take(MaxWords)
      recommendations <- forWords(words)
    } yield recommendations
  }

  def forUser(userId: UUID)(implicit ec: ExecutionContext): Future[Seq[ProductShort]] = {
    for {
      recentProducts <- recentlyOrdered(userId)
      allWords = recentProducts.flatMap(wordsInName)
      randomWords = Random.shuffle(allWords).take(MaxWords)
      recommendations <- forWords(randomWords)
    } yield recommendations
  }

  private def recentlyOrdered(userId: UUID)(implicit ec: ExecutionContext): Future[Seq[ProductDetails]] = {
    for {
      orders <- ordersService.historical(userId)
      recentProductIds = orders.flatMap(_.items).map(_.product).take(MaxRecentProducts)
      recentProducts <- Future.traverse(recentProductIds)(productService.findDetailed)
    } yield recentProducts
  }

  private def forWords(words: Seq[String])(implicit ec: ExecutionContext): Future[Seq[ProductShort]] = {
    for {
      recommended <- Future.traverse(words)(findSimilarProducts)
      backup <- findDefaultProducts
    } yield {
      (recommended.flatMap(_.data).sortBy(_.averageRating).reverse ++ backup.data).take(MaxRecommendations)
    }
  }

  private def findSimilarProducts(word: String)(implicit ec: ExecutionContext): Future[Paginated[ProductShort]] = {
    val filtering = Filtering(text      = word.some, minRating = MinRating.some)
    productService.list(filtering, firstPage)
  }

  private def findDefaultProducts(implicit ec: ExecutionContext) = {
    productService.list(Filtering(minRating = MinRating.some), firstPage)
  }

  private def wordsInName(product: ProductDetails) = {
    product.name.split(" ").toSeq
  }

  private val firstPage = Pagination(size = MaxRecommendations, page = 1)

}
