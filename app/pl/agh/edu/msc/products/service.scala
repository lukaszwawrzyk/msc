package pl.agh.edu.msc.products

import javax.inject.{ Inject, Singleton }

import pl.agh.edu.msc.availability.AvailabilityService
import pl.agh.edu.msc.pricing.PricingService
import pl.agh.edu.msc.products.Filtering.PriceRange
import pl.agh.edu.msc.review.{ Rating, ReviewService }

import scala.concurrent.{ ExecutionContext, Future }

case class Sorting(byNameAsc: Boolean)

case class Filtering(
  text:       Option[String] = None,
  priceRange: Option[PriceRange] = None,
  minRating:  Option[Rating] = None
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
      repoView <- productRepository.find(id)
      rating <- reviewService.averageRating(id)
      reviews <- reviewService.reviews(id)
    } yield {
      ProductDetails(
        repoView.name,
        repoView.cachedPrice,
        repoView.photo,
        repoView.description,
        rating.orElse(repoView.cachedAverageRating),
        reviews = reviews,
        availability = None,
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