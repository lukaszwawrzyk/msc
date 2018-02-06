package pl.agh.edu.msc.products

import pl.agh.edu.msc.common.IntegrationTest
import pl.agh.edu.msc.review.{ Rating, ReviewRepository, Review }
import cats.syntax.option._

class ProductDetailsSpec extends IntegrationTest {

  private val productsService = inject[ProductService]
  private val productsRepository = inject[ProductRepository]
  private val reviewRepository = inject[ReviewRepository]

  it should "fetch detailed product view" in {
    // GIVEN
    val id = productsRepository.insert(ProductRepoView(
      name = "apples",
      cachedPrice = Money(10),
      photo = None,
      cachedAverageRating = Rating(3.75).some,
      description = "Some nice apples..."
    )).await()
    reviewRepository.insert(id, Review("Mark", "very nice", Rating(4.0)))
    reviewRepository.insert(id, Review("John", "cool",      Rating(5.0)))
    reviewRepository.insert(id, Review("Bob",  "I liked",   Rating(4.5)))

    // WHEN
    val detailedProduct = productsService.find(id).await()

    // THEN
    detailedProduct shouldBe ProductDetails(
      name = "apples",
      price = Money(10),
      photo = None,
      description = "Some nice apples...",
      averageRating = Rating(4.5).some,
      reviews = Seq(
        Review("Mark", "very nice", Rating(4.0)),
        Review("John", "cool",      Rating(5.0)),
        Review("Bob",  "I liked",   Rating(4.5))
      ),
      availability = None,
      id = id
    )
  }

}
