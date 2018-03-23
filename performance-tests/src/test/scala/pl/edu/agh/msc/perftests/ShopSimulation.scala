package pl.edu.agh.msc.perftests

import io.gatling.core.Predef._
import io.gatling.http.Predef._

// todo add pause
class ShopSimulation extends Simulation {

	private val baseUrl = "http://localhost:9000"

  val random = new Random(lastProductId = 501)

	val headers = Map(
		"Origin" -> baseUrl
	)

	val httpProtocol = http
		.baseURL(baseUrl)
//		.inferHtmlResources()
		.acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
		.acceptEncodingHeader("gzip, deflate")
		.acceptLanguageHeader("pl-PL,pl;q=0.9,en-US;q=0.8,en;q=0.7")
		.userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36 OPR/51.0.2830.55")
  	.headers(headers)




	setUp(new BuyingScenario(random).create.inject(atOnceUsers(100))).protocols(httpProtocol)
}