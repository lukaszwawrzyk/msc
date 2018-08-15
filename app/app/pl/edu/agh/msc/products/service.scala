package pl.edu.agh.msc.products

import javax.inject.{ Inject, Singleton }
import cats.data.OptionT
import cats.instances.future._
import cats.syntax.apply._
import pl.edu.agh.msc.availability.AvailabilityService
import pl.edu.agh.msc.pricing.{ Money, PricingService }
import pl.edu.agh.msc.products.Filtering.PriceRange
import pl.edu.agh.msc.review.{ Rating, ReviewService }
import pl.edu.agh.msc.utils.Cache
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect.ClassTag

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
  pricingService:      PricingService,
  cache:               Cache
) {

  def findDetailed(
    id: ProductId
  )(implicit ec: ExecutionContext): Future[ProductDetails] = {
    def cached[A: ClassTag](namespace: String, get: ProductId => Future[A]): Future[A] = cache.cached(namespace, expiration = 10.minutes)(id)(get)
    (
      cached("prod-repo", productRepository.find),
      cached("avg-rate", reviewService.averageRating),
      cached("reviews", reviewService.find),
      cached("price", pricingService.find),
      cached("availability", availabilityService.find)
    ).mapN { (product, rating, reviews, price, availability) =>
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
  }

  def findShort(
    id: ProductId
  )(implicit ec: ExecutionContext): Future[ProductShort] = {
    cache.cached(namespace  = "prod-short", expiration = 10.minutes)(id)(findShortNonCached)
  }

  def updateCache(
    id: ProductId
  )(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      product <- productRepository.find(id)
      rating <- reviewService.averageRating(id)
      price <- pricingService.find(id)
      _ <- updateCachedData(id, product, rating, price)
    } yield ()
  }

  def findShortNonCached(
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
    cache.cached(namespace  = "prod-list", expiration = 30.minutes)((filtering, pagination, sorting))((listNonCached _).tupled)
  }

  def listNonCached(
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