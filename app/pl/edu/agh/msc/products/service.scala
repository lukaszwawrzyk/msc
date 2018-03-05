package pl.edu.agh.msc.products

import java.net.URL
import javax.inject.{ Inject, Singleton }

import cats.data.OptionT
import cats.instances.future._
import pl.edu.agh.msc.availability.AvailabilityService
import pl.edu.agh.msc.pricing.{ Money, PricingService }
import pl.edu.agh.msc.products.Filtering.PriceRange
import pl.edu.agh.msc.review.{ Rating, ReviewService }

import scala.concurrent.{ ExecutionContext, Future }

case class Sorting(byNameAsc: Boolean)
object Sorting {
  val Default = Sorting(byNameAsc = true)
}

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

  def findDetailed(
    id: ProductId
  )(implicit ec: ExecutionContext): Future[ProductDetails] = {
    for {
      product <- productRepository.find(id)
      rating <- reviewService.averageRating(id)
      reviews <- reviewService.find(id)
      price <- pricingService.find(id)
      availability <- availabilityService.find(id)
      _ <- updateCachedData(id, product, rating, price)
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

  def findShort(
    id: ProductId
  )(implicit ec: ExecutionContext): Future[ProductShort] = {
    for {
      product <- productRepository.find(id)
    } yield {
      ProductShort(
        product.name,
        product.cachedPrice,
        product.photo,
        product.cachedAverageRating,
        id
      )
    }
  }

  def price(id: ProductId)(implicit ec: ExecutionContext): Future[Money] = {
    OptionT(pricingService.find(id)).getOrElseF(productRepository.find(id).map(_.cachedPrice))
  }

  def list(
    filtering:  Filtering,
    pagination: Pagination,
    sorting:    Sorting    = Sorting.Default
  )(implicit ec: ExecutionContext): Future[Paginated[ProductShort]] = {
    productRepository.list(filtering, pagination, sorting)
  }

  private def updateCachedData(
    id:      ProductId,
    product: ProductRepoView,
    rating:  Option[Rating],
    price:   Option[Money]
  )(implicit ec: ExecutionContext) = {
    if (rating != product.cachedAverageRating || price.exists(_ != product.cachedPrice)) {
      val updatedProduct = product.copy(
        cachedAverageRating = rating,
        cachedPrice         = price.getOrElse(product.cachedPrice)
      )
      productRepository.update(id, updatedProduct)
    } else Future.unit
  }

}