package pl.edu.agh.msc.auth.user

import java.util.UUID

import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }

case class User(
  id:        UUID,
  loginInfo: LoginInfo,
  firstName: Option[String],
  lastName:  Option[String],
  email:     Option[String]
) extends Identity {

  def fullName: Option[String] = {
    (firstName, lastName) match {
      case (Some(f), Some(l)) => Some(f + " " + l)
      case (Some(f), None)    => Some(f)
      case (None, Some(l))    => Some(l)
      case _                  => None
    }
  }
}
