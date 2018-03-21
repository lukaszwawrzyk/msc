package pl.edu.agh.msc.utils

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID

import slick.jdbc.JdbcProfile

trait SlickTypeMappings {

  protected val profile: JdbcProfile

  import profile.api._

  protected implicit lazy val localDateTimeMapping = MappedColumnType.base[LocalDateTime, Timestamp](Timestamp.valueOf, _.toLocalDateTime)

}