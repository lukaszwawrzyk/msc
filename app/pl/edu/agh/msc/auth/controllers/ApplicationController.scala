package pl.edu.agh.msc.auth.controllers

import javax.inject.Inject

import _root_.controllers.AssetsFinder
import com.mohiva.play.silhouette.api.{ LogoutEvent, Silhouette }
import org.webjars.play.WebJarsUtil
import pl.edu.agh.msc.auth._
import pl.edu.agh.msc.auth.infra.DefaultEnv
import play.api.i18n.I18nSupport
import play.api.mvc.{ AbstractController, ControllerComponents }

class ApplicationController @Inject() (
  components: ControllerComponents,
  silhouette: Silhouette[DefaultEnv]
)(
  implicit
  webJarsUtil: WebJarsUtil,
  assets:      AssetsFinder
) extends AbstractController(components) with I18nSupport {

  def index = silhouette.SecuredAction { implicit request =>
    Redirect(pl.edu.agh.msc.ui.controllers.routes.LandingPageController.view)
  }

  def profile = silhouette.SecuredAction { implicit request =>
    Ok(views.html.profile(request.identity))
  }

  def signOut = silhouette.SecuredAction.async { implicit request =>
    val result = Redirect(routes.ApplicationController.index())
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, result)
  }

}
