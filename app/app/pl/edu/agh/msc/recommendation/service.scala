package pl.edu.agh.msc.recommendation

import java.util.UUID

import cats.syntax.option._
import com.google.inject.{ Inject, Singleton }
import pl.edu.agh.msc.orders.OrdersService
import pl.edu.agh.msc.products._
import pl.edu.agh.msc.review.Rating

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

  def default(): Seq[ProductShort] = {
    findDefaultProducts.data
  }

  def forProduct(productId: ProductId, max: Int): Seq[ProductShort] = {
    val product = productService.findShort(productId)
    val words = wordsInName(product).take(MaxWords)
    forWords(words, excluding = Set(productId), max)
  }

  def forUser(userId: UUID): Seq[ProductShort] = {
    val recentProducts = recentlyOrdered(userId)
    val allWords = recentProducts.flatMap(wordsInName)
    val randomWords = Random.shuffle(allWords).take(MaxWords)
    forWords(randomWords, excluding = recentProducts.map(_.id).toSet)
  }

  private def recentlyOrdered(userId: UUID): Seq[ProductShort] = {
    val orders = ordersService.historical(userId)
    val recentProductIds = orders.flatMap(_.items).map(_.product).take(MaxRecentProducts)
    recentProductIds.map(productService.findShort)
  }

  private def forWords(words: Seq[String], excluding: Set[ProductId], max: Int = MaxRecommendations): Seq[ProductShort] = {
    val recommended = words.map(findSimilarProducts(_, max))
    val backup = findDefaultProducts
    (recommended.flatMap(_.data).sortBy(_.averageRating).reverse ++ backup.data)
      .distinct
      .filterNot(p => excluding.contains(p.id))
      .take(max)
  }

  private def findSimilarProducts(word: String, max: Int): Paginated[ProductShort] = {
    val filtering = Filtering(text      = word.some, minRating = MinRating.some)
    productService.list(filtering, firstPage(max))
  }

  private def findDefaultProducts = {
    productService.list(Filtering(minRating = MinRating.some), firstPage(MaxRecommendations))
  }

  private def wordsInName(product: ProductShort) = {
    product.name.split(" ").toSeq
  }

  private def firstPage(size: Int) = Pagination(size = size, page = 1)

}
