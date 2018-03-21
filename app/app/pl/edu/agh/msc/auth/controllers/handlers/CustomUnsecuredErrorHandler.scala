package pl.edu.agh.msc.auth.controllers.handlers

import com.mohiva.play.silhouette.api.actions.UnsecuredErrorHandler
import pl.edu.agh.msc.auth.controllers.routes
import play.api.mvc.RequestHeader
import play.api.mvc.Results._

import scala.concurrent.Future

class CustomUnsecuredErrorHandler extends UnsecuredErrorHandler {

  override def onNotAuthorized(implicit request: RequestHeader) = {
    Future.successful(Redirect(routes.ApplicationController.index()))
  }

}
