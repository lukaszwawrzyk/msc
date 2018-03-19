package pl.edu.agh.msc.auth.controllers

import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.util.{ Clock, Credentials }
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers._
import javax.inject.Inject
import net.ceedubs.ficus.Ficus._
import pl.edu.agh.msc.auth._
import pl.edu.agh.msc.auth.controllers.forms.SignInForm
import pl.edu.agh.msc.auth.user.UserService
import pl.edu.agh.msc.utils.SecuredController
import play.api.Configuration
import play.api.i18n.Messages
import play.api.mvc.{ AnyContent, Request }

import scala.concurrent.Future
import scala.concurrent.duration._

class SignInController @Inject() (
  sc:                  SecuredController,
  userService:         UserService,
  credentialsProvider: CredentialsProvider,
  configuration:       Configuration,
  clock:               Clock
) {

  import sc._

  def view = Unsecured { implicit request =>
    Future.successful(Ok(views.html.signIn(SignInForm.form)))
  }

  def submit = Unsecured { implicit request =>
    SignInForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.signIn(form))),
      data => {
        val credentials = Credentials(data.email, data.password)
        for {
          loginInfo <- credentialsProvider.authenticate(credentials)
          user <- userService.retrieve(loginInfo).map(_.getOrElse(throw new IdentityNotFoundException("Couldn't find user")))
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
