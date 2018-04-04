package pl.edu.agh.msc.auth.controllers

import java.util.UUID

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import com.mohiva.play.silhouette.impl.providers._
import javax.inject.Inject
import pl.edu.agh.msc.auth._
import pl.edu.agh.msc.auth.controllers.forms.SignUpForm
import pl.edu.agh.msc.auth.token.AuthTokenService
import pl.edu.agh.msc.auth.user.{ User, UserService }
import pl.edu.agh.msc.utils._
import play.api.i18n.Messages

import scala.concurrent.Future

class SignUpController @Inject() (
  sc:                     SecuredController,
  userService:            UserService,
  authInfoRepository:     AuthInfoRepository,
  authTokenService:       AuthTokenService,
  passwordHasherRegistry: PasswordHasherRegistry
) {

  import sc._

  def view = Unsecured { implicit request =>
    Future.successful(Ok(views.html.signUp(SignUpForm.form)))
  }

  def submit = Unsecured { implicit request =>
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
              _ <- Future.sequence(Seq(
                authInfoRepository.add(loginInfo, hashedPassword),
                authTokenService.create(savedUser.id)
              ))
              _ <- Future.successful(silhouette.env.eventBus.publish(SignUpEvent(savedUser, request)))
            } yield {
              Redirect(routes.SignInController.view()).flashing("info" -> Messages("sign.up.success"))
            }
        }
      }
    )
  }
}
