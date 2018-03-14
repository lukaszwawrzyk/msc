package pl.edu.agh.msc.ui.controllers

import javax.inject.Inject

import pl.edu.agh.msc.payment.{ PaymentId, PaymentService }
import pl.edu.agh.msc.ui.views
import pl.edu.agh.msc.utils.SecuredController

import scala.concurrent.Future

class PaymentController @Inject() (
  sc:             SecuredController,
  paymentService: PaymentService
) {
  import sc._

  def view(id: PaymentId) = UserAware.async { implicit request =>
    for {
      payment <- paymentService.get(id)
    } yield {
      Ok(views.html.paymentForm(payment, id))
    }
  }

  def pay(id: PaymentId) = UserAware.async { implicit request =>
    paymentService.pay(id)
    Future.successful(Redirect(routes.LandingPageController.view())
      .flashing("success" -> "Thank you for your payment"))
  }

}
