package pl.edu.agh.msc.auth.token

import java.time.LocalDateTime
import java.util.UUID

case class AuthToken(
  id:     UUID,
  userID: UUID,
  expiry: LocalDateTime
)
