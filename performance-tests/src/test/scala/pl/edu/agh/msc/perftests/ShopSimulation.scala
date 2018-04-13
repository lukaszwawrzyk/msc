package pl.edu.agh.msc.perftests

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class BaseSimulation extends Simulation {
  private val baseUrl = "http://192.168.1.2:9000"

  val random = new Random(lastProductId = 501)

  val headers = Map("Origin" -> baseUrl)

  val httpProtocol = http
    .baseURL(baseUrl)
    .inferHtmlResources(WhiteList(baseUrl + "/.*"))
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("pl-PL,pl;q=0.9,en-US;q=0.8,en;q=0.7")
    .userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36 OPR/51.0.2830.55")
    .headers(headers)

  def buying = new BuyingScenario(random)
  def browsing = new BrowsingScenario(random)
}

class StandardUsage extends BaseSimulation {

  val simulationTime = 10.minutes
  val pauseTime = 1.second.toMillis

  setUp(
    browsing.repeating.inject(
      rampUsers(1400) over simulationTime
    ).customPauses(pauseTime),
    buying.repeating.inject(
      rampUsers(100) over simulationTime
    ).customPauses(pauseTime)
  ).protocols(httpProtocol).maxDuration(simulationTime)

}

// this will cause ConnectException with timeout, application being not able to even handle this number of connections
class ALotOfUsers extends BaseSimulation {

  val simulationTime = 5.minutes
  val pauseTime = 500.millis

  setUp(
    browsing.single.inject(
      constantUsersPerSec(50) during simulationTime
    ).customPauses(pauseTime.toMillis),
    buying.single.inject(
      constantUsersPerSec(10) during simulationTime
    ).customPauses(pauseTime.toMillis)
  ).protocols(httpProtocol).maxDuration(simulationTime)

}

class HighLoad extends BaseSimulation {

  val simulationTime = 5.minutes
  val pauseTime = 500.millis

  setUp(
    browsing.repeating.inject(
      rampUsers(500) over simulationTime
    ).customPauses(pauseTime.toMillis),
    buying.repeating.inject(
      rampUsers(50) over simulationTime
    ).customPauses(pauseTime.toMillis)
  ).protocols(httpProtocol).maxDuration(simulationTime)

}



class HighBuyerLoad extends BaseSimulation {

  val simulationTime = 5.minutes
  val pauseTime = 200.millis.toMillis

  setUp(
    buying.repeating.inject(
      rampUsers(10000) over simulationTime
    ).customPauses(pauseTime)
  ).protocols(httpProtocol).maxDuration(simulationTime)

}
