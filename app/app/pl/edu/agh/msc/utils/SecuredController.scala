package pl.edu.agh.msc.utils

import javax.inject.{ Inject, Singleton }
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.actions.{ SecuredRequest, UserAwareRequest }
import controllers.AssetsFinder
import org.webjars.play.WebJarsUtil
import pl.edu.agh.msc.auth.infra.DefaultEnv
import pl.edu.agh.msc.auth.user.User
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

abstract class SecuredController(
  components:     ControllerComponents,
  val silhouette: Silhouette[DefaultEnv]
)(
  implicit
  val webJarsUtil: WebJarsUtil,
  val assets:      AssetsFinder,
  val ec:          ExecutionContext
) extends AbstractController(components) with I18nSupport {

  type UserReq = SecuredRequest[DefaultEnv, AnyContent]
  type MaybeUserReq = UserAwareRequest[DefaultEnv, AnyContent]
  type NoUserReq = Request[AnyContent]
  type Res = Future[Result]

  def Secured(block: UserReq => Res): Action[AnyContent]
  def UserAware(block: MaybeUserReq => Res): Action[AnyContent]
  def Unsecured(block: NoUserReq => Res): Action[AnyContent]

  implicit def unwrapUserFromSecured(implicit request: UserReq): Option[User] = {
    Some(request.identity)
  }

  implicit def unwrapUserFromUserAware(implicit request: MaybeUserReq): Option[User] = {
    request.identity
  }

}

class AsyncSecuredController @Inject() (
  components:  ControllerComponents,
  silhouette:  Silhouette[DefaultEnv],
  webJarsUtil: WebJarsUtil,
  assets:      AssetsFinder,
  ec:          ExecutionContext
) extends SecuredController(components, silhouette)(webJarsUtil, assets, ec) {

  override def Secured(block: UserReq => Res): Action[AnyContent] = silhouette.SecuredAction.async(block)
  override def UserAware(block: MaybeUserReq => Res): Action[AnyContent] = silhouette.UserAwareAction.async(block)
  override def Unsecured(block: NoUserReq => Res): Action[AnyContent] = silhouette.UnsecuredAction.async(block)

}

// likely not feasible solution, can cause deadlocks
class BlockingSecuredController @Inject() (
  components:  ControllerComponents,
  silhouette:  Silhouette[DefaultEnv],
  webJarsUtil: WebJarsUtil,
  assets:      AssetsFinder,
  ec:          ExecutionContext
) extends SecuredController(components, silhouette)(webJarsUtil, assets, ec) {

  override def Secured(block: UserReq => Res): Action[AnyContent] = silhouette.SecuredAction.async(blocking(block))
  override def UserAware(block: MaybeUserReq => Res): Action[AnyContent] = silhouette.UserAwareAction.async(blocking(block))
  override def Unsecured(block: NoUserReq => Res): Action[AnyContent] = silhouette.UnsecuredAction.async(blocking(block))

  def blocking[Req](f: Req => Res): Req => Res = { req: Req =>
    Future.successful(Await.result(f(req), 10.minutes))
  }

}