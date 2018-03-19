package pl.edu.agh.msc.auth.controllers

import com.mohiva.play.silhouette.api.LogoutEvent
import javax.inject.Inject
import pl.edu.agh.msc.auth._
import pl.edu.agh.msc.utils.SecuredController

import scala.concurrent.Future

class ApplicationController @Inject() (sc: SecuredController) {
  import sc._

  def index = UserAware { implicit request =>
    Future.successful(Redirect(pl.edu.agh.msc.ui.controllers.routes.LandingPageController.view()))
  }

  def profile = Secured { implicit request =>
    Future.successful(Ok(views.html.profile(request.identity)))
  }

  def signOut = Secured { implicit request =>
    val result = Redirect(routes.ApplicationController.index())
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, result)
  }

}
