package pl.agh.edu.msc.products

import javax.inject.{ Inject, Singleton }

import pl.agh.edu.msc.availability.AvailabilityService
import pl.agh.edu.msc.pricing.PricingService
import pl.agh.edu.msc.products.Filtering.PriceRange
import pl.agh.edu.msc.review.ReviewService

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
  )(implicit ec: ExecutionContext): Future[ProductFullView] = {
    for {
      repoView <- productRepository.find(id)
    } yield {
      ProductFullView(
        repoView.name,
        repoView.cachedPrice,
        repoView.photo,
        repoView.description,
        repoView.cachedAverageRating,
        reviews = Seq.empty,
        availability = None,
        id
      )
    }
  }

  def list(
    filtering:  Filtering,
    pagination: Pagination,
    sorting:    Sorting
  )(implicit ec: ExecutionContext): Future[Paginated[ProductListView]] = {
    productRepository.list(filtering, pagination, sorting)
  }

}