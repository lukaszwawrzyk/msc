package pl.edu.agh.msc.reviews

import java.time.{ LocalDate, LocalDateTime, LocalTime }

import pl.edu.agh.msc.common.IntegrationTest
import pl.edu.agh.msc.pricing.Money
import pl.edu.agh.msc.products.{ ProductRepoView, ProductRepository }
import pl.edu.agh.msc.review.ReviewService
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

class ReviewsSpec extends IntegrationTest with ReviewFactories {

  private val reviewService = behaviorOf[ReviewService]

  private val productsRepository = withHelpOf[ProductRepository]

  it should "return all added reviews for item" in {
    // GIVEN
    val product = createProduct()

    // WHEN
    reviewService.add(product, createReview("Mark", "nice", rating = 4)).await()
    reviewService.add(product, createReview("John", "crap", rating = 1)).await()
    reviewService.add(product, createReview("Jake", "cool", rating = 3)).await()
    val reviews = reviewService.find(product).await()

    // THEN
    reviews should contain theSameElementsAs Seq(
      createReview("Mark", "nice", rating = 4),
      createReview("John", "crap", rating = 1),
      createReview("Jake", "cool", rating = 3)
    )
  }

  it should "return 3 latest reviews" in {
    // GIVEN
    val product = createProduct()
    reviewService.add(product, createReview(author = "a", date = at(10, 40))).await()
    reviewService.add(product, createReview(author = "b", date = at(11, 20))).await()
    reviewService.add(product, createReview(author = "c", date = at(12, 10))).await()
    reviewService.add(product, createReview(author = "d", date = at(13, 30))).await()
    reviewService.add(product, createReview(author = "e", date = at(14, 20))).await()

    // WHEN
    val reviews = reviewService.latest(limit = 3).await()

    // THEN
    reviews shouldBe Seq(
      createReview(author = "e", date = at(14, 20)) -> product,
      createReview(author = "d", date = at(13, 30)) -> product,
      createReview(author = "c", date = at(12, 10)) -> product
    )
  }

  private def at(hour: Int, min: Int) = LocalDateTime.of(LocalDate.of(2017, 2, 1), LocalTime.of(hour, min))

  private def createProduct() = {
    productsRepository.insert(ProductRepoView("apples", Money(10), photo = None, cachedAverageRating = None, description = "")).await()
  }
}
