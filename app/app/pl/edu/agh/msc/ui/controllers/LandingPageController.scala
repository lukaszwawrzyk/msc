package pl.edu.agh.msc.ui.controllers

import javax.inject.Inject

import pl.edu.agh.msc.products.ProductService
import pl.edu.agh.msc.recommendation.RecommendationService
import pl.edu.agh.msc.review.ReviewService
import pl.edu.agh.msc.ui.views
import pl.edu.agh.msc.utils.SecuredController

import scala.concurrent.Future

class LandingPageController @Inject() (
  sc:                    SecuredController,
  recommendationService: RecommendationService,
  reviewService:         ReviewService,
  productService:        ProductService
) {
  import sc._

  private val MaxReviews = 10

  def view = UserAware { implicit request =>
    val recommendations = request.identity
      .fold(recommendationService.default())(user => recommendationService.forUser(user.id))
    val latestReviews = reviewService.latest(MaxReviews)
    val reviewsWithProducts = latestReviews.map {
      case (review, productId) => review -> productService.findShort(productId)
    }
    Ok(views.html.landing(recommendations, reviewsWithProducts))
  }

}
