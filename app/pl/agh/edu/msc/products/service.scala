package pl.agh.edu.msc.products

import javax.inject.{ Inject, Singleton }

import pl.agh.edu.msc.products.Filter.PriceRange

import scala.concurrent.{ ExecutionContext, Future }

case class Filter(
  text:       Option[String],
  priceRange: Option[PriceRange],
  minRating:  Option[Rating]
)

object Filter {
  case class PriceRange(from: Money, to: Money)
}

case class Pagination(size: Int, page: Int)

case class Paginated[A](pagination: Pagination, total: Int, data: Seq[A])

@Singleton
class ProductService @Inject() (productRepository: ProductRepository) {

  def list(
    filter: Filter,
    pagination: Pagination
  )(implicit ec: ExecutionContext): Future[Paginated[ProductListView]] = {
    productRepository.list(filter, pagination)
  }

}