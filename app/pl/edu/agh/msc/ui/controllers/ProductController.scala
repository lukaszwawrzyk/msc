package pl.edu.agh.msc.ui.controllers

import javax.inject.Inject

import _root_.controllers.AssetsFinder
import com.mohiva.play.silhouette.api.Silhouette
import org.webjars.play.WebJarsUtil
import pl.edu.agh.msc.auth.infra.DefaultEnv
import pl.edu.agh.msc.products.ProductService
import play.api.i18n.I18nSupport
import play.api.mvc.{ AbstractController, ControllerComponents }

import scala.concurrent.Future

class ProductController @Inject() (
  components:     ControllerComponents,
  silhouette:     Silhouette[DefaultEnv],
  productService: ProductService
)(
  implicit
  webJarsUtil: WebJarsUtil,
  assets:      AssetsFinder
) extends AbstractController(components) with I18nSupport {

  def list = silhouette.SecuredAction.async { implicit request =>
    Future.successful(Ok( /*views.html.home(request.identity)*/ ""))
  }

}
