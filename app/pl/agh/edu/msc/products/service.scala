package pl.agh.edu.msc.products

import javax.inject.{ Inject, Singleton }

import pl.agh.edu.msc.products.Filtering.PriceRange

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

@Singleton
class ProductService @Inject() (productRepository: ProductRepository) {

  def list(
    filtering:  Filtering,
    pagination: Pagination,
    sorting:    Sorting
  )(implicit ec: ExecutionContext): Future[Paginated[ProductListView]] = {
    productRepository.list(filtering, pagination, sorting)
  }

}