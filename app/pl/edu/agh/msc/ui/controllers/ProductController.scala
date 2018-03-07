package pl.edu.agh.msc.ui.controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import controllers.AssetsFinder
import org.webjars.play.WebJarsUtil
import pl.edu.agh.msc.auth.infra.DefaultEnv
import pl.edu.agh.msc.auth.user.User
import pl.edu.agh.msc.products.{ ProductId, ProductService }
import pl.edu.agh.msc.ui.views
import play.api.i18n.I18nSupport
import play.api.mvc.{ AbstractController, AnyContent, ControllerComponents }

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

  def list = silhouette.UserAwareAction.async { implicit request =>
    Future.successful(Ok( /*views.html.home(request.identity)*/ ""))
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
