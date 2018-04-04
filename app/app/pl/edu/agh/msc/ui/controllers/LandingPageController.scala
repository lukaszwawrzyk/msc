package pl.edu.agh.msc.ui.controllers

import javax.inject.Inject
import pl.edu.agh.msc.auth.user.User
import pl.edu.agh.msc.products.ProductService
import pl.edu.agh.msc.recommendation.RecommendationService
import pl.edu.agh.msc.review.ReviewService
import pl.edu.agh.msc.ui.views
import pl.edu.agh.msc.utils.SecuredController
import cats.syntax.apply._
import cats.instances.future._
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
    (latestReviews, recommendations(request.identity)).mapN { (reviews, recommendations) =>
      Ok(views.html.landing(recommendations, reviews))
    }
  }

  private def latestReviews = {
    for {
      latestReviews <- reviewService.latest(MaxReviews)
      reviewsWithProducts <- Future.traverse(latestReviews){
        case (review, productId) => productService.findShort(productId).map(review -> _)
      }
    } yield reviewsWithProducts
  }

  private def recommendations(maybeUser: Option[User]) = {
    maybeUser
      .fold(recommendationService.default())(user => recommendationService.forUser(user.id))
  }

}
