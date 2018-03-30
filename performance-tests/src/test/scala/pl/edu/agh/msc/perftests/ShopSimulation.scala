package pl.edu.agh.msc.perftests

import io.gatling.commons.validation.Validation
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class ShopSimulation extends Simulation {

	private val baseUrl = "http://192.168.1.1:9000"

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

	val simulationTime = 7.minutes
	val pauseTime = 1.second.toMillis

	setUp(
		new BuyingScenario(random).create.inject(
			rampUsers(1000) over simulationTime
		).customPauses(pauseTime),
	new BrowsingScenario(random).create.inject(
			rampUsers(50) over simulationTime
		).customPauses(pauseTime)
	).protocols(httpProtocol).maxDuration(simulationTime)
}
