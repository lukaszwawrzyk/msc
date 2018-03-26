package pl.edu.agh.msc.products

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
  case class PriceRange(from: Option[Money], to: Option[Money])
}

case class Pagination(size: Int, page: Int)

case class Paginated[A](pagination: Pagination, totalPages: Int, data: Seq[A]) {
  def next = Option(pagination.page + 1).filter(_ <= totalPages)
  def prev = Option(pagination.page - 1).filter(_ >= 1)
}

@Singleton class ProductService @Inject() (
  productRepository:   ProductRepository,
  reviewService:       ReviewService,
  availabilityService: AvailabilityService,
  pricingService:      PricingService
) {

  def findDetailed(
    id: ProductId
  ): ProductDetails = {
    val product = productRepository.find(id)
    val rating = reviewService.averageRating(id)
    val reviews = reviewService.find(id)
    val price = pricingService.find(id)
    val availability = availabilityService.find(id)
    val photo = product.photo.map { p =>
      p.toString.replaceFirst("(\\.[A-Za-z]+)$", "_full$1")
    }
    ProductDetails(
      product.name,
      price.getOrElse(product.cachedPrice),
      photo,
      product.description,
      rating.orElse(product.cachedAverageRating),
      reviews,
      availability,
      id
    )
  }

  def updateCache(
    id: ProductId
  ): Unit = {
    val product = productRepository.find(id)
    val rating = reviewService.averageRating(id)
    val price = pricingService.find(id)
    updateCachedData(id, product, rating, price)
  }

  def findShort(
    id: ProductId
  ): ProductShort = {
    val product = productRepository.find(id)
    ProductShort(
      product.name,
      product.cachedPrice,
      product.photo,
      product.cachedAverageRating,
      id
    )
  }

  def price(id: ProductId): Money = {
    pricingService.find(id).getOrElse(productRepository.find(id).cachedPrice)
  }

  def list(
    filtering:  Filtering,
    pagination: Pagination,
    sorting:    Sorting    = Sorting.Default
  ): Paginated[ProductShort] = {
    productRepository.list(filtering, pagination, sorting)
  }

  private def updateCachedData(
    id:      ProductId,
    product: ProductRepoView,
    rating:  Option[Rating],
    price:   Option[Money]
  ): Unit = {
    if (rating != product.cachedAverageRating || price.exists(_ != product.cachedPrice)) {
      val updatedProduct = product.copy(
        cachedAverageRating = rating,
        cachedPrice         = price.getOrElse(product.cachedPrice)
      )
      productRepository.update(id, updatedProduct)
    }
  }

}