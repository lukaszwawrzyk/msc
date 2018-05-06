package pl.edu.agh.msc.ui.controllers

import java.net.URL

import javax.inject.Inject
import pl.edu.agh.msc.cart.{ Cart, CartItem }
import pl.edu.agh.msc.orders._
import pl.edu.agh.msc.payment.{ PaymentRequest, PaymentService, Product }
import pl.edu.agh.msc.products.{ ProductId, ProductService }
import pl.edu.agh.msc.ui.views
import pl.edu.agh.msc.utils._
import play.api.data.Forms._
import play.api.data.{ Form, Mapping }
import play.api.mvc.Result

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
      e => Future.successful(BadRequest(e.errors.toString)),
      orderDraft => ordersService.saveDraft(orderDraft, request.identity.id).flatMap(orderDetailsView)
    )
  }

  def view(id: OrderId) = Secured { implicit request =>
    ordersService.find(id).flatMap(orderDetailsView)
  }

  def list() = Secured { implicit request =>
    for {
      orders <- ordersService.historical(request.identity.id)
    } yield Ok(views.html.orderList(orders))
  }

  def confirm(id: OrderId) = Secured { implicit request =>
    for {
      _ <- ordersService.confirm(id)
      order <- ordersService.find(id)
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

  def paid(id: OrderId) = UserAware { implicit request =>
    ordersService.paymentConfirmed(id).map(_ => Ok)
  }

  private def orderDetailsView(order: Order)(implicit r: UserReq): Res = {
    for {
      products <- buildMap(order.items.map(_.product))(productService.findShort)
    } yield {
      Ok(views.html.orderDetails(order, products))
    }
  }

}
