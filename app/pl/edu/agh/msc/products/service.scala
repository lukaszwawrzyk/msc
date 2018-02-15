package pl.edu.agh.msc.products

import javax.inject.{ Inject, Singleton }

import pl.edu.agh.msc.availability.AvailabilityService
import pl.edu.agh.msc.pricing.{ Money, PricingService }
import pl.edu.agh.msc.products.Filtering.PriceRange
import pl.edu.agh.msc.review.{ Rating, ReviewService }

import scala.concurrent.{ ExecutionContext, Future }

case class Sorting(byNameAsc: Boolean)

case class Filtering(
  text:       Option[String]     = None,
  priceRange: Option[PriceRange] = None,
  minRating:  Option[Rating]     = None
)

object Filtering {
  case class PriceRange(from: Money, to: Money)
}

case class Pagination(size: Int, page: Int)

case class Paginated[A](pagination: Pagination, totalPages: Int, data: Seq[A])

@Singleton class ProductService @Inject() (
  productRepository:   ProductRepository,
  reviewService:       ReviewService,
  availabilityService: AvailabilityService,
  pricingService:      PricingService
) {

  def find(
    id: ProductId
  )(implicit ec: ExecutionContext): Future[ProductDetails] = {
    for {
      product <- productRepository.find(id)
      rating <- reviewService.averageRating(id)
      reviews <- reviewService.find(id)
      price <- pricingService.find(id)
      availability <- availabilityService.find(id)
    } yield {
      ProductDetails(
        product.name,
        price.getOrElse(product.cachedPrice),
        product.photo,
        product.description,
        rating.orElse(product.cachedAverageRating),
        reviews,
        availability,
        id
      )
    }
  }

  def list(
    filtering:  Filtering,
    pagination: Pagination,
    sorting:    Sorting
  )(implicit ec: ExecutionContext): Future[Paginated[ProductListItem]] = {
    productRepository.list(filtering, pagination, sorting)
  }

}