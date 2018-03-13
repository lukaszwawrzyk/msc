package pl.edu.agh.msc.ui.controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import controllers.AssetsFinder
import org.webjars.play.WebJarsUtil
import pl.edu.agh.msc.auth.infra.DefaultEnv
import pl.edu.agh.msc.auth.user.User
import pl.edu.agh.msc.cart.CartService
import pl.edu.agh.msc.products.{ ProductId, ProductService }
import pl.edu.agh.msc.ui.views
import play.api.i18n.I18nSupport
import play.api.mvc.{ AbstractController, AnyContent, ControllerComponents }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

class CartController @Inject() (
  components:     ControllerComponents,
  silhouette:     Silhouette[DefaultEnv],
  cartService:    CartService,
  productService: ProductService
)(
  implicit
  webJarsUtil: WebJarsUtil,
  assets:      AssetsFinder,
  ec:          ExecutionContext
) extends AbstractController(components) with I18nSupport {

  def view = silhouette.SecuredAction.async { implicit request =>
    for {
      cart <- cartService.get(request.identity.id)
      products <- buildMap(cart.items.map(_.product))(productService.findShort)
    } yield {
      Ok(views.html.cart(cart, products))
    }
  }

  def add(productId: ProductId) = silhouette.SecuredAction.async { implicit request =>
    extractNumberField("amount") match {
      case Some(amount) =>
        for {
          _ <- cartService.add(request.identity.id, productId, amount)
        } yield {
          Redirect(routes.ProductController.details(productId)).flashing("success" -> "Added to cart")
        }
      case None =>
        Future.successful(BadRequest)
    }
  }

  private def extractNumberField(name: String)(implicit request: SecuredRequest[DefaultEnv, AnyContent]) = {
    request.body.asFormUrlEncoded.flatMap(_.get(name)).flatMap(_.headOption).flatMap(v => Try(v.toInt).toOption)
  }

  private implicit def unwrapUser(implicit request: SecuredRequest[DefaultEnv, AnyContent]): Option[User] = {
    Some(request.identity)
  }

  private def buildMap[K, V](keys: Seq[K])(getValue: K => Future[V]): Future[Map[K, V]] = {
    Future.traverse(keys.distinct)(key => getValue(key).map(key -> _)).map(_.toMap)
  }

}
