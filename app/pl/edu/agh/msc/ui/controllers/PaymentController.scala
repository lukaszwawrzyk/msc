package pl.edu.agh.msc.ui.controllers

import javax.inject.Inject

import pl.edu.agh.msc.payment.{ PaymentId, PaymentService }
import pl.edu.agh.msc.ui.views
import pl.edu.agh.msc.utils.SecuredController

class PaymentController @Inject() (
  sc:             SecuredController,
  paymentService: PaymentService
) {
  import sc._

  def view(id: PaymentId) = UserAware.async { implicit request =>
    for {
      payment <- paymentService.get(id)
    } yield {
      Ok(views.html.paymentForm(payment))
    }
  }

}
