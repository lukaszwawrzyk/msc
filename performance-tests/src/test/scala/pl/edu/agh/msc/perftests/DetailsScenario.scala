package pl.edu.agh.msc.perftests

import CommonActions._

import io.gatling.core.Predef._
import scala.concurrent.duration._

class DetailsScenario(random: Random) {

  val feeder = Iterator.continually(Map("product" -> random.productId()))

  def repeating = scenario("Details scenario")
    .feed(feeder)
    .forever(productDetails("${product}").pause(3.seconds))

}
