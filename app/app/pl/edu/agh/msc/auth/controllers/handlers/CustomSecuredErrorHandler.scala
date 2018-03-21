package pl.edu.agh.msc.auth.controllers.handlers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.actions.SecuredErrorHandler
import pl.edu.agh.msc.auth.controllers.routes
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.mvc.RequestHeader
import play.api.mvc.Results._

import scala.concurrent.Future

class CustomSecuredErrorHandler @Inject() (val messagesApi: MessagesApi) extends SecuredErrorHandler with I18nSupport {

  override def onNotAuthenticated(implicit request: RequestHeader) = {
    Future.successful(Redirect(routes.SignInController.view()))
  }

  override def onNotAuthorized(implicit request: RequestHeader) = {
    Future.successful(Redirect(routes.SignInController.view()).flashing("error" -> Messages("access.denied")))
  }
}
