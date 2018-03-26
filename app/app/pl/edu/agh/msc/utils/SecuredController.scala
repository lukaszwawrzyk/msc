package pl.edu.agh.msc.utils

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.{ SecuredRequest, UserAwareRequest }
import controllers.AssetsFinder
import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.auth.infra.DefaultEnv
import pl.edu.agh.msc.auth.user.User
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton class SecuredController @Inject() (
  components:     ControllerComponents,
  val silhouette: Silhouette[DefaultEnv]
)(
  implicit
  val assets: AssetsFinder,
  val ec:     ExecutionContext
) extends AbstractController(components) with I18nSupport {

  type UserReq = SecuredRequest[DefaultEnv, AnyContent]
  type MaybeUserReq = UserAwareRequest[DefaultEnv, AnyContent]
  type NoUserReq = Request[AnyContent]
  type Res = Result

  def Secured(block: UserReq => Res): Action[AnyContent] = silhouette.SecuredAction(block)
  def UserAware(block: MaybeUserReq => Res): Action[AnyContent] = silhouette.UserAwareAction(block)
  def Unsecured(block: NoUserReq => Res): Action[AnyContent] = silhouette.UnsecuredAction(block)

  implicit def unwrapUserFromSecured(implicit request: UserReq): Option[User] = {
    Some(request.identity)
  }

  implicit def unwrapUserFromUserAware(implicit request: MaybeUserReq): Option[User] = {
    request.identity
  }

}