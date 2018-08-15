package pl.edu.agh.msc.ui.controllers

import javax.inject.Inject
import cats.syntax.option._
import cats.syntax.apply._
import cats.instances.future._
import pl.edu.agh.msc.pricing.Money
import pl.edu.agh.msc.products.Filtering.PriceRange
import pl.edu.agh.msc.products.{ Filtering, Pagination, ProductId, ProductService }
import pl.edu.agh.msc.recommendation.RecommendationService
import pl.edu.agh.msc.review.{ Rating, Review, ReviewService }
import pl.edu.agh.msc.ui.views
import pl.edu.agh.msc.utils.{ SecuredController, Time }
import play.api.data.Forms._
import play.api.data.{ Form, Mapping }

import scala.concurrent.Future

class ProductController @Inject() (
  sc:                    SecuredController,
  productService:        ProductService,
  reviewService:         ReviewService,
  recommendationService: RecommendationService,
  time:                  Time
) {

  import sc._

  private val DefaultPageSize = 20

  private case class RowForm(
    author:  String,
    content: String,
    rating:  Rating
  )

  private val ratingMapping: Mapping[Rating] = number.transform(n => Rating(n.toDouble), _.value.toInt)

  private val reviewForm: Form[RowForm] = Form(
    mapping(
      "author" -> nonEmptyText,
      "content" -> nonEmptyText,
      "rating" -> ratingMapping
    )(RowForm.apply)(RowForm.unapply)
  )

  def list(
    text:      Option[String],
    minPrice:  Option[Int],
    maxPrice:  Option[Int],
    minRating: Option[Int],
    size:      Option[Int],
    page:      Option[Int]
  ) = UserAware { implicit request =>
    val filtering = Filtering(text, PriceRange(minPrice.map(Money(_)), maxPrice.map(Money(_))).some, minRating.map(r => Rating(r.toDouble)))
    val pagination = Pagination(size.getOrElse(DefaultPageSize), page.getOrElse(1))
    for {
      paginated <- productService.list(filtering, pagination)
    } yield {
      Ok(views.html.productList(paginated, text, minPrice, maxPrice, minRating))
    }
  }

  def details(id: ProductId) = UserAware { implicit request =>
    (
      productService.findDetailed(id),
      recommendationService.forProduct(id, max = 4)
    ).mapN { (product, recommendations) =>
      Ok(views.html.productDetails(product, recommendations))
    }
  }

  def review(id: ProductId) = Secured { implicit request =>
    reviewForm.bindFromRequest.fold(
      e => Future.successful(BadRequest(e.errors.toString)),
      reviewForm => reviewService.add(id, Review(reviewForm.author, reviewForm.content, reviewForm.rating, time.now())).map { _ =>
        Redirect(routes.ProductController.details(id))
      }
    )
  }

}
