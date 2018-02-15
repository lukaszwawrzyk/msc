package pl.edu.agh.msc.auth.controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.util.{ Clock, Credentials }
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers._
import pl.edu.agh.msc.auth.user.{ User, UserService }
import net.ceedubs.ficus.Ficus._
import org.webjars.play.WebJarsUtil
import pl.edu.agh.msc.auth._
import _root_.controllers.AssetsFinder
import pl.edu.agh.msc.auth.controllers.forms.SignInForm
import pl.edu.agh.msc.auth.infra.DefaultEnv
import play.api.Configuration
import play.api.i18n.{ I18nSupport, Messages }
import play.api.mvc.{ AbstractController, AnyContent, ControllerComponents, Request }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

class SignInController @Inject() (
  components:          ControllerComponents,
  silhouette:          Silhouette[DefaultEnv],
  userService:         UserService,
  credentialsProvider: CredentialsProvider,
  configuration:       Configuration,
  clock:               Clock
)(
  implicit
  webJarsUtil: WebJarsUtil,
  assets:      AssetsFinder,
  ex:          ExecutionContext
) extends AbstractController(components) with I18nSupport {

  def view = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(views.html.signIn(SignInForm.form)))
  }

  def submit = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    SignInForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.signIn(form))),
      data => {
        val credentials = Credentials(data.email, data.password)
        for {
          loginInfo <- credentialsProvider.authenticate(credentials)
          user: User <- userService.retrieve(loginInfo).map(_.getOrElse(throw new IdentityNotFoundException("Couldn't find user")))
          authenticator <- silhouette.env.authenticatorService.create(loginInfo) map {
            case authenticator if data.rememberMe =>
              val c = configuration.underlying
              authenticator.copy(
                expirationDateTime = clock.now + c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
                idleTimeout        = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout"),
                cookieMaxAge       = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.cookieMaxAge")
              )
            case authenticator => authenticator
          }
          _ <- Future.successful(silhouette.env.eventBus.publish(LoginEvent(user, request)))
          cookie <- silhouette.env.authenticatorService.init(authenticator)
          response <- silhouette.env.authenticatorService.embed(cookie, Redirect(routes.ApplicationController.index()))
        } yield response
      }.recover {
        case _: ProviderException =>
          Redirect(routes.SignInController.view()).flashing("error" -> Messages("invalid.credentials"))
      }
    )
  }
}
