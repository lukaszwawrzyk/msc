package pl.edu.agh.msc.utils

import javax.inject.{ Singleton, Inject }

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.{ SecuredRequest, UserAwareRequest }
import controllers.AssetsFinder
import org.webjars.play.WebJarsUtil
import pl.edu.agh.msc.auth.infra.DefaultEnv
import pl.edu.agh.msc.auth.user.User
import play.api.i18n.I18nSupport
import play.api.mvc.{ AbstractController, AnyContent, ControllerComponents }

import scala.concurrent.ExecutionContext

@Singleton class SecuredController @Inject() (
  components:     ControllerComponents,
  val silhouette:     Silhouette[DefaultEnv],
)(
  implicit
  val webJarsUtil: WebJarsUtil,
  val assets:      AssetsFinder,
  val ec:          ExecutionContext
) extends AbstractController(components) with I18nSupport {

  def Secured = silhouette.SecuredAction
  def UserAware = silhouette.UserAwareAction
  def Unsecured = silhouette.UnsecuredAction

  implicit def unwrapUserFromSecured(implicit request: SecuredRequest[DefaultEnv, AnyContent]): Option[User] = {
    Some(request.identity)
  }

  implicit def unwrapUserFromUserAware(implicit request: UserAwareRequest[DefaultEnv, AnyContent]): Option[User] = {
    request.identity
  }

}
