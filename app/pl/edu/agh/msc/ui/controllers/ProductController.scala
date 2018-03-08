package pl.edu.agh.msc.ui.controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import controllers.AssetsFinder
import org.webjars.play.WebJarsUtil
import pl.edu.agh.msc.auth.infra.DefaultEnv
import pl.edu.agh.msc.auth.user.User
import pl.edu.agh.msc.pricing.Money
import pl.edu.agh.msc.products.Filtering.PriceRange
import pl.edu.agh.msc.products.{ Filtering, Pagination, ProductId, ProductService }
import pl.edu.agh.msc.review.Rating
import pl.edu.agh.msc.ui.views
import play.api.i18n.I18nSupport
import play.api.mvc.{ AbstractController, AnyContent, ControllerComponents }
import cats.syntax.option._

import scala.concurrent.{ ExecutionContext, Future }

class ProductController @Inject() (
  components:     ControllerComponents,
  silhouette:     Silhouette[DefaultEnv],
  productService: ProductService
)(
  implicit
  webJarsUtil: WebJarsUtil,
  assets:      AssetsFinder,
  ec:          ExecutionContext
) extends AbstractController(components) with I18nSupport {

  private val DefaultPageSize = 20

  def list(
    text:      Option[String],
    minPrice:  Option[Int],
    maxPrice:  Option[Int],
    minRating: Option[Int],
    size:      Option[Int],
    page:      Option[Int]
  ) = silhouette.UserAwareAction.async { implicit request =>
    val filtering = Filtering(text, PriceRange(minPrice.map(Money(_)), maxPrice.map(Money(_))).some, minRating.map(r => Rating(r.toDouble)))
    val pagination = Pagination(size.getOrElse(DefaultPageSize), page.getOrElse(1))
    for {
      paginated <- productService.list(filtering, pagination)
    } yield {
      Ok(views.html.productList(paginated))
    }
  }

  def details(id: ProductId) = silhouette.UserAwareAction.async { implicit request =>
    for {
      product <- productService.findDetailed(id)
    } yield {
      Ok(views.html.productDetails(product))
    }
  }

  private implicit def unwrapUser(implicit request: UserAwareRequest[DefaultEnv, AnyContent]): Option[User] = {
    request.identity
  }

}
