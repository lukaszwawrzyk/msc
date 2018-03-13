package pl.edu.agh.msc.ui.controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import controllers.AssetsFinder
import org.webjars.play.WebJarsUtil
import pl.edu.agh.msc.auth.infra.DefaultEnv
import pl.edu.agh.msc.auth.user.User
import pl.edu.agh.msc.cart.{ Cart, CartItem, CartService }
import pl.edu.agh.msc.orders.{ Address, OrderDraft, OrderId, OrdersService }
import pl.edu.agh.msc.products.{ ProductId, ProductService }
import pl.edu.agh.msc.ui.views
import play.api.data.Forms.{ mapping, seq, _ }
import play.api.data.{ Form, Mapping }
import play.api.i18n.I18nSupport
import play.api.mvc.{ AbstractController, AnyContent, ControllerComponents }

import scala.concurrent.{ ExecutionContext, Future }

class OrderController @Inject() (
  components:     ControllerComponents,
  silhouette:     Silhouette[DefaultEnv],
  ordersService: OrdersService
)(
  implicit
  webJarsUtil: WebJarsUtil,
  assets:      AssetsFinder,
  ec:          ExecutionContext
) extends AbstractController(components) with I18nSupport {

  private val productIdMapping: Mapping[ProductId] = longNumber.transform(ProductId(_), _.value)

  private val cartItem: Mapping[CartItem] = mapping(
    "product" -> productIdMapping,
    "amount" -> number
  )(CartItem.apply)(CartItem.unapply)

  private val cartMapping: Mapping[Cart] = mapping(
    "items" -> seq(cartItem).verifying("Order must not be empty", _.nonEmpty)
  )(Cart.apply)(Cart.unapply)

  private val addressMapping: Mapping[Address] = mapping(
    "fullName"      -> nonEmptyText,
    "streetAddress" -> nonEmptyText,
    "zipCode"       -> nonEmptyText,
    "city"          -> nonEmptyText,
    "country"       -> nonEmptyText,
  )(Address.apply)(Address.unapply)

  private val orderForm: Form[OrderDraft] = Form(
    mapping(
      "cart"    -> cartMapping,
      "address" -> addressMapping
    )(OrderDraft.apply)(OrderDraft.unapply)
  )


  def draft = silhouette.SecuredAction.async { implicit request =>
    orderForm.bindFromRequest.fold(
      e => Future.successful(BadRequest(e.errors.toString)),
      orderDraft => ordersService.saveDraft(orderDraft, request.identity.id).map { id =>
        Redirect(routes.OrderController.show(id))
      }
    )
  }

  def show(id: OrderId) = silhouette.SecuredAction.async { implicit request =>
    Future.successful(Ok(id.toString))
  }

  private implicit def unwrapUser(implicit request: SecuredRequest[DefaultEnv, AnyContent]): Option[User] = {
    Some(request.identity)
  }

}
