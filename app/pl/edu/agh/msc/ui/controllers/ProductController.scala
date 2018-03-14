package pl.edu.agh.msc.ui.controllers

import javax.inject.Inject

import cats.syntax.option._
import pl.edu.agh.msc.pricing.Money
import pl.edu.agh.msc.products.Filtering.PriceRange
import pl.edu.agh.msc.products.{ Filtering, Pagination, ProductId, ProductService }
import pl.edu.agh.msc.review.Rating
import pl.edu.agh.msc.ui.views
import pl.edu.agh.msc.utils.SecuredController

class ProductController @Inject() (
  sc:             SecuredController,
  productService: ProductService
) {

  import sc._

  private val DefaultPageSize = 20

  def list(
    text:      Option[String],
    minPrice:  Option[Int],
    maxPrice:  Option[Int],
    minRating: Option[Int],
    size:      Option[Int],
    page:      Option[Int]
  ) = UserAware.async { implicit request =>
    val filtering = Filtering(text, PriceRange(minPrice.map(Money(_)), maxPrice.map(Money(_))).some, minRating.map(r => Rating(r.toDouble)))
    val pagination = Pagination(size.getOrElse(DefaultPageSize), page.getOrElse(1))
    for {
      paginated <- productService.list(filtering, pagination)
    } yield {
      Ok(views.html.productList(paginated, text, minPrice, maxPrice, minRating))
    }
  }

  def details(id: ProductId) = UserAware.async { implicit request =>
    for {
      product <- productService.findDetailed(id)
    } yield {
      Ok(views.html.productDetails(product))
    }
  }

}
