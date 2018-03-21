package pl.edu.agh.msc.auth.infra

import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import pl.edu.agh.msc.auth.user.User

trait DefaultEnv extends Env {
  type I = User
  type A = CookieAuthenticator
}
