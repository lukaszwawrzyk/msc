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

  private val cartItemMapping: Mapping[CartItem] = mapping(
    "product" -> productIdMapping,
    "amount" -> number
  )(CartItem.apply)(CartItem.unapply)

  private val cartMapping: Mapping[Cart] = mapping(
    "items" -> seq(cartItemMapping).verifying("Order must not be empty", _.nonEmpty)
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


  def draft = Secured { implicit request =>
    orderForm.bindFromRequest.fold(
      e => BadRequest(e.errors.toString),
      orderDraft => {
        val id = ordersService.saveDraft(orderDraft, request.identity.id)
        Redirect(routes.OrderController.view(id))
      }
    )
  }

  def view(id: OrderId) = Secured { implicit request =>
    val order = ordersService.find(id)
    val products = buildMap(order.items.map(_.product))(productService.findShort)
    Ok(views.html.orderDetails(order, products))
  }

  def list() = Secured { implicit request =>
    val orders = ordersService.historical(request.identity.id)
    Ok(views.html.orderList(orders))
  }

  def confirm(id: OrderId) = Secured { implicit request =>
    val order = ordersService.find(id)
    if (order.buyer != request.identity.id) {
      BadRequest("You don't have such order")
    } else {
      ordersService.confirm(id)
      val products = buildMap(order.items.map(_.product))(productService.findShort)
      val paymentId = paymentService.create(PaymentRequest(
        totalPrice = order.totalPrice,
        email = request.identity.email.getOrElse(""),
        address = order.address,
        products = order.items.map(item => Product(products(item.product).name, item.price, item.amount)),
        returnUrl = new URL(routes.OrderController.paid(id).absoluteURL())
      ))
      Redirect(routes.PaymentController.view(paymentId))
    }
  }

  def paid(id: OrderId) = UserAware { implicit request =>
    ordersService.paymentConfirmed(id)
    Ok
  }

}
