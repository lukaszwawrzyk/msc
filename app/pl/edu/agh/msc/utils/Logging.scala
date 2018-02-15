package pl.edu.agh.msc.utils

trait Logging {
  val logger = play.api.Logger(this.getClass)
}
