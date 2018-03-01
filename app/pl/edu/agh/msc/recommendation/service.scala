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
  ordersService: OrdersService
) {

  private val MaxWords = 4
  private val MaxRecommendations = 5
  private val MaxRecentProducts = 5
  private val MinRating = Rating(4)

  def forProduct(productId: ProductId)(implicit ec: ExecutionContext): Future[Seq[ProductListItem]] = {
    for {
      product <- productService.find(productId)
      words = wordsInName(product).take(MaxWords)
      recommended <- Future.traverse(words)(word => productService.list(Filtering(text = word.some, minRating = MinRating.some), Pagination(size = MaxRecommendations, page = 1)))
      backup <- productService.list(Filtering(), Pagination(size = MaxRecommendations, page = 1))
    } yield {
      (recommended.flatMap(_.data).sortBy(_.averageRating).reverse ++ backup.data).take(MaxRecommendations)
    }
  }

  def forUser(userId: UUID)(implicit ec: ExecutionContext): Future[Seq[ProductListItem]] = {
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
      recentProducts <- Future.traverse(recentProductIds)(productService.find)
    } yield recentProducts
  }

  private def forWords(words: Seq[String])(implicit ec: ExecutionContext): Future[Seq[ProductListItem]] = {
    for {
      recommended <- Future.traverse(words)(findSimilarProducts)
      backup <- findDefaultProducts
    } yield {
      (recommended.flatMap(_.data).sortBy(_.averageRating).reverse ++ backup.data).take(MaxRecommendations)
    }
  }


  private def findSimilarProducts(word: String)(implicit ec: ExecutionContext): Future[Paginated[ProductListItem]] = {
    val filtering = Filtering(text = word.some, minRating = MinRating.some)
    val firstPage = Pagination(size = MaxRecommendations, page = 1)
    productService.list(filtering, firstPage)
  }

  private def findDefaultProducts(implicit ec: ExecutionContext) = {
    productService.list(Filtering(minRating = MinRating.some), Pagination(size = MaxRecommendations, page = 1))
  }

  private def wordsInName(product: ProductDetails) = {
    product.name.split(" ").toSeq
  }
}
