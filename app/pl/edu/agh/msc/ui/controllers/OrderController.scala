package pl.edu.agh.msc.ui.controllers

import java.net.URL
import javax.inject.Inject

import pl.edu.agh.msc.cart.{ Cart, CartItem }
import pl.edu.agh.msc.orders.{ Address, OrderDraft, OrderId, OrdersService }
import pl.edu.agh.msc.payment.{ PaymentRequest, PaymentService, Product }
import pl.edu.agh.msc.products.{ ProductId, ProductService }
import pl.edu.agh.msc.ui.views
import pl.edu.agh.msc.utils._
import play.api.data.Forms._
import play.api.data.{ Form, Mapping }

import scala.concurrent.Future

class OrderController @Inject() (
  sc:     SecuredController,
  ordersService:  OrdersService,
  productService: ProductService,
  paymentService: PaymentService
){

  import sc._

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


  def draft = Secured.async { implicit request =>
    orderForm.bindFromRequest.fold(
      e => Future.successful(BadRequest(e.errors.toString)),
      orderDraft => ordersService.saveDraft(orderDraft, request.identity.id).map { id =>
        Redirect(routes.OrderController.view(id))
      }
    )
  }

  def view(id: OrderId) = Secured.async { implicit request =>
    for {
      order <- ordersService.find(id)
      products <- buildMap(order.items.map(_.product))(productService.findShort)
    } yield {
      Ok(views.html.orderConfirmation(order, products))
    }
  }

  def confirm(id: OrderId) = Secured.async { implicit request =>
    for {
      order <- ordersService.find(id)
      res <- if (order.buyer != request.identity.id) {
        Future.successful(BadRequest("You don't have such order"))
      } else {
        for {
          _ <- ordersService.confirm(id)
          products <- buildMap(order.items.map(_.product))(productService.findShort)
          paymentId <- paymentService.create(PaymentRequest(
            order.totalPrice,
            request.identity.email.getOrElse(""),
            order.address,
            order.items.map(item => Product(products(item.product).name, item.price, item.amount)),
            new URL(routes.OrderController.paid(id).absoluteURL())
          ))
        } yield Redirect(routes.PaymentController.view(paymentId))
      }
    } yield res
  }

  def paid(id: OrderId) = UserAware.async { implicit request =>
    ordersService.paymentConfirmed(id).map(_ => Ok)
  }

}
