package pl.edu.agh.msc.ui.controllers

import javax.inject.Inject

import pl.edu.agh.msc.cart.CartService
import pl.edu.agh.msc.products.{ ProductId, ProductService }
import pl.edu.agh.msc.ui.views
import pl.edu.agh.msc.utils._
import play.api.mvc.{ AnyContent, Request }

import scala.concurrent.Future
import scala.util.Try

class CartController @Inject() (
  sc:             SecuredController,
  cartService:    CartService,
  productService: ProductService
) {

  import sc._

  def view = Secured { implicit request =>
    val cart = cartService.get(request.identity.id)
    val products = buildMap(cart.items.map(_.product))(productService.findShort)
    Ok(views.html.cart(cart, products))
  }

  def add(productId: ProductId) = Secured { implicit request =>
    extractNumberField("amount") match {
      case Some(amount) =>
        cartService.add(request.identity.id, productId, amount)
        Redirect(routes.ProductController.details(productId)).flashing("success" -> "Added to cart")
      case None =>
        BadRequest
    }
  }

  private def extractNumberField(name: String)(implicit request: Request[AnyContent]) = {
    request.body.asFormUrlEncoded.flatMap(_.get(name)).flatMap(_.headOption).flatMap(v => Try(v.toInt).toOption)
  }

}
