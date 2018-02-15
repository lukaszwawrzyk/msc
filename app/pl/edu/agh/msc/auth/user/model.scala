package pl.edu.agh.msc.auth.user

import java.util.UUID

import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }

case class User(
  userID:    UUID,
  loginInfo: LoginInfo,
  firstName: Option[String],
  lastName:  Option[String],
  fullName:  Option[String],
  email:     Option[String],
  activated: Boolean
) extends Identity {

  def name = fullName.orElse {
    firstName -> lastName match {
      case (Some(f), Some(l)) => Some(f + " " + l)
      case (Some(f), None)    => Some(f)
      case (None, Some(l))    => Some(l)
      case _                  => None
    }
  }
}
