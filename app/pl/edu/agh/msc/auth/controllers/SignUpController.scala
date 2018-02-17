package pl.edu.agh.msc.auth.controllers

import java.util.UUID
import javax.inject.Inject

import _root_.controllers.AssetsFinder
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import com.mohiva.play.silhouette.impl.providers._
import pl.edu.agh.msc.auth.token.AuthTokenService
import pl.edu.agh.msc.auth.user.{ User, UserService }
import org.webjars.play.WebJarsUtil
import pl.edu.agh.msc.auth._
import pl.edu.agh.msc.auth.controllers.forms.SignUpForm
import pl.edu.agh.msc.auth.infra.DefaultEnv
import play.api.i18n.{ I18nSupport, Messages }
import play.api.mvc.{ AbstractController, AnyContent, ControllerComponents, Request }

import scala.concurrent.{ ExecutionContext, Future }

class SignUpController @Inject() (
  components:             ControllerComponents,
  silhouette:             Silhouette[DefaultEnv],
  userService:            UserService,
  authInfoRepository:     AuthInfoRepository,
  authTokenService:       AuthTokenService,
  passwordHasherRegistry: PasswordHasherRegistry
)(
  implicit
  webJarsUtil: WebJarsUtil,
  assets:      AssetsFinder,
  ex:          ExecutionContext
) extends AbstractController(components) with I18nSupport {

  def view = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(views.html.signUp(SignUpForm.form)))
  }

  def submit = silhouette.UnsecuredAction.async { implicit request: Request[AnyContent] =>
    SignUpForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.signUp(form))),
      data => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
        userService.retrieve(loginInfo).flatMap {
          case Some(_) =>
            Future.successful(Redirect(routes.SignInController.view()).flashing("error" -> "User with this email already exists"))
          case None =>
            val user = User(
              id        = UUID.randomUUID(),
              loginInfo = loginInfo,
              firstName = Some(data.firstName),
              lastName  = Some(data.lastName),
              email     = Some(data.email)
            )
            for {
              savedUser <- userService.save(user)
              hashedPassword = passwordHasherRegistry.current.hash(data.password)
              _ <- authInfoRepository.add(loginInfo, hashedPassword)
              _ <- authTokenService.create(savedUser.id)
              _ <- Future.successful(silhouette.env.eventBus.publish(SignUpEvent(savedUser, request)))
            } yield {
              Redirect(routes.SignInController.view()).flashing("info" -> Messages("sign.up.success"))
            }
        }
      }
    )
  }
}
