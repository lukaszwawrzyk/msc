package pl.edu.agh.msc.ui.controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import controllers.AssetsFinder
import org.webjars.play.WebJarsUtil
import pl.edu.agh.msc.auth.infra.DefaultEnv
import pl.edu.agh.msc.auth.user.User
import pl.edu.agh.msc.products.ProductService
import pl.edu.agh.msc.recommendation.RecommendationService
import pl.edu.agh.msc.review.ReviewService
import play.api.i18n.I18nSupport
import play.api.mvc.{ AbstractController, AnyContent, ControllerComponents }
import pl.edu.agh.msc.ui.views

import scala.concurrent.{ ExecutionContext, Future }

class LandingPageController @Inject() (
  components:            ControllerComponents,
  silhouette:            Silhouette[DefaultEnv],
  recommendationService: RecommendationService,
  reviewService:         ReviewService,
  productService:        ProductService
)(
  implicit
  webJarsUtil: WebJarsUtil,
  assets:      AssetsFinder,
  ex:          ExecutionContext
) extends AbstractController(components) with I18nSupport {

  def view = silhouette.UserAwareAction.async { implicit request =>
    for {
      recommendations <- request.identity.fold(recommendationService.default())(user => recommendationService.forUser(user.id))
      latestReviews <- reviewService.latest(10)
      reviewsWithProducts <- Future.traverse(latestReviews){ case (review, productId) => productService.findShort(productId).map(review -> _) }
    } yield {
      Ok(views.html.landing(recommendations, reviewsWithProducts))
    }
  }

  private implicit def unwrapUser(implicit request: UserAwareRequest[DefaultEnv, AnyContent]): Option[User] = {
    request.identity
  }

}
