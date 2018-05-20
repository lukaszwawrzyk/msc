package pl.edu.agh.msc.perftests

import io.gatling.core.Predef._
import io.gatling.core.structure.PopulationBuilder
import io.gatling.http.Predef._

import scala.concurrent.duration._

class BaseSimulation extends Simulation {
  private val baseUrl = "http://mscth.eu-west-2.elasticbeanstalk.com"

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

  object scenario {
    def buying = new BuyingScenario(random)
    def browsing = new BrowsingScenario(random)
    def details = new DetailsScenario(random)
  }

  def simulationTime = 5.minutes

  override def setUp(populationBuilders: List[PopulationBuilder]): SetUp = {
    super.setUp(populationBuilders).protocols(httpProtocol).maxDuration(simulationTime)
  }
}

class StandardUsage extends BaseSimulation {

  override val simulationTime = 10.minutes
  val pauseTime = 1.second.toMillis

  setUp(
    scenario.browsing.repeating.inject(
      rampUsers(1400) over simulationTime
    ).customPauses(pauseTime),
    scenario.buying.repeating.inject(
      rampUsers(100) over simulationTime
    ).customPauses(pauseTime)
  )

}

// this will cause ConnectException with timeout, application being not able to even handle this number of connections
class ALotOfUsers extends BaseSimulation {

  val pauseTime = 500.millis

  setUp(
    scenario.browsing.single.inject(
      constantUsersPerSec(50) during simulationTime
    ).customPauses(pauseTime.toMillis),
    scenario.buying.single.inject(
      constantUsersPerSec(10) during simulationTime
    ).customPauses(pauseTime.toMillis)
  )

}

class HighLoad extends BaseSimulation {

  val pauseTime = 500.millis

  setUp(
    scenario.browsing.repeating.inject(
      rampUsers(500) over simulationTime
    ).customPauses(pauseTime.toMillis),
    scenario.buying.repeating.inject(
      rampUsers(50) over simulationTime
    ).customPauses(pauseTime.toMillis)
  )

}


class AutoScaling extends BaseSimulation {

  val pauseTime = 500.millis
  val maxUsers = 3000
  override val simulationTime = 8.minutes


  setUp(
    scenario.browsing.repeating.inject(
      rampUsers(maxUsers) over simulationTime
    ).customPauses(pauseTime.toMillis),
    scenario.buying.repeating.inject(
      rampUsers(maxUsers / 10) over simulationTime
    ).customPauses(pauseTime.toMillis)
  )

}


/*
class CircuitBreakerTest extends BaseSimulation {

  override val simulationTime = 3.minutes
  val pauseTime = 200.millis.toMillis

  setUp(
    scenario.browsing.single.inject(
      constantUsersPerSec(200) during simulationTime
    ).customPauses(pauseTime).throttle(
      reachRps(200) in 1.minute,
      holdFor(30.seconds),
      reachRps(100) in 1.minute,
      holdFor(1.minute)
    )
  )

}
*/


class CircuitBreakerTest extends BaseSimulation {

  override val simulationTime = 4.minutes
  val pauseTime = 200.millis.toMillis

	setUp(
		scenario.browsing.single.inject(
			constantUsersPerSec(15) during 2.minutes,
			constantUsersPerSec( 6) during 2.minutes
		).customPauses(pauseTime)
	)

}


class HighBuyerLoad extends BaseSimulation {

  val pauseTime = 200.millis.toMillis

  setUp(
    scenario.buying.repeating.inject(
      rampUsers(10000) over simulationTime
    ).customPauses(pauseTime)
  )

}

class DisplayDetails extends BaseSimulation {

  setUp(
    scenario.details.repeating.inject(
      rampUsers(500) over simulationTime
    )
  )

}