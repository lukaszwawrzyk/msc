package pl.edu.agh.msc.auth.controllers

import com.mohiva.play.silhouette.api.LogoutEvent
import javax.inject.Inject
import pl.edu.agh.msc.auth._
import pl.edu.agh.msc.utils._

class ApplicationController @Inject() (sc: SecuredController) {
  import sc._

  def index = UserAware { implicit request =>
    Redirect(pl.edu.agh.msc.ui.controllers.routes.LandingPageController.view())
  }

  def profile = Secured { implicit request =>
    Ok(views.html.profile(request.identity))
  }

  def signOut = Secured { implicit request =>
    val result = Redirect(routes.ApplicationController.index())
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, result).await()
  }

}
