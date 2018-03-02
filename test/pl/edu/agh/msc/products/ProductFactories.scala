package pl.edu.agh.msc.products

import pl.edu.agh.msc.common.IntegrationTest
import pl.edu.agh.msc.pricing.{ Money, PriceRepository }
import pl.edu.agh.msc.review.{ Rating, ReviewRepository }
import pl.edu.agh.msc.reviews.ReviewFactories

import scala.concurrent.Future

trait ProductFactories extends ReviewFactories { this: IntegrationTest =>

  private val productService = inject[ProductService]
  private val productsRepository = inject[ProductRepository]
  private val pricesRepository = inject[PriceRepository]
  private val reviewRepository = inject[ReviewRepository]

  def createAndSaveProduct(
    name:   String,
    price:  Money,
    rating: Int    = 4
  ): Future[ProductId] = {
    for {
      id <- productsRepository.insert(ProductRepoView(
        name,
        Money(0),
        photo               = None,
        cachedAverageRating = None,
        description         = ""
      ))
      _ <- pricesRepository.save(id, price)
      _ <- reviewRepository.insert(id, createReview(rating = rating))
      _ <- productService.find(id) // triggers cache update
    } yield id
  }

}
