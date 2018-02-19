package pl.edu.agh.msc.utils

import java.time.LocalDateTime
import javax.inject.Singleton

trait Time {
  def now(): LocalDateTime
}

@Singleton class RealTime extends Time {
  override def now(): LocalDateTime = LocalDateTime.now()
}
