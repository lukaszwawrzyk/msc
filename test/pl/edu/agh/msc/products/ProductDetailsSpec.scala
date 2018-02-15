package pl.edu.agh.msc.products

import cats.syntax.option._
import pl.edu.agh.msc.availability.{ Availability, AvailabilityRepository }
import pl.edu.agh.msc.common.IntegrationTest
import pl.edu.agh.msc.pricing.{ Money, PriceRepository }
import pl.edu.agh.msc.review.{ Rating, ReviewRepository }
import pl.edu.agh.msc.reviews.ReviewFactories

class ProductDetailsSpec extends IntegrationTest with ReviewFactories {

  private val productsService = behaviorOf[ProductService]

  private val productsRepository = withHelpOf[ProductRepository]
  private val reviewRepository = withHelpOf[ReviewRepository]
  private val priceRepository = withHelpOf[PriceRepository]
  private val availabilityRepository = withHelpOf[AvailabilityRepository]

  it should "fetch detailed product view" in {
    // GIVEN
    val id = productsRepository.insert(ProductRepoView(
      name                = "apples",
      cachedPrice         = Money(10),
      photo               = None,
      cachedAverageRating = Rating(3.75).some,
      description         = "Some nice apples..."
    )).await()
    reviewRepository.insert(id, createReview("Mark", "very nice", rating = 4)).await()
    reviewRepository.insert(id, createReview("John", "cool", rating = 5)).await()
    reviewRepository.insert(id, createReview("Bob", "I liked", rating = 3)).await()
    priceRepository.save(id, Money(12)).await()
    availabilityRepository.save(id, Availability(stock = 42))

    // WHEN
    val detailedProduct = productsService.find(id).await()

    // THEN
    detailedProduct shouldBe ProductDetails(
      name          = "apples",
      price         = Money(12),
      photo         = None,
      description   = "Some nice apples...",
      averageRating = Rating(4).some,
      reviews       = Seq(
        createReview("Mark", "very nice", rating = 4),
        createReview("John", "cool", rating = 5),
        createReview("Bob", "I liked", rating = 3)
      ),
      availability  = Availability(stock = 42).some,
      id            = id
    )
  }

}
