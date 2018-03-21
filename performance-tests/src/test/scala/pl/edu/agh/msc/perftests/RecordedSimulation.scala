package pl.edu.agh.msc.perftests

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

import scala.util.Random

class RecordedSimulation extends Simulation {

	private val baseUrl = "http://localhost:9000"

	val httpProtocol = http
		.baseURL(baseUrl)
		.inferHtmlResources()
		.acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
		.acceptEncodingHeader("gzip, deflate")
		.acceptLanguageHeader("pl-PL,pl;q=0.9,en-US;q=0.8,en;q=0.7")
		.userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36 OPR/51.0.2830.55")

	val getHeaders = Map("Upgrade-Insecure-Requests" -> "1")

	val postHeaders = Map(
		"Origin" -> baseUrl,
		"Upgrade-Insecure-Requests" -> "1"
  )

  val userFeeder = Iterator.continually(
    Map("email" -> (Random.alphanumeric.take(20).mkString + "@foo.com"))
  )

	val buyingScenario = scenario("Buying Scenario")
		.exec(http("show signup form")
			.get("/signUp")
			.headers(getHeaders))
		.pause(15)
		.exec(http("post signup form")
			.post("/signUp")
			.headers(postHeaders)
			.formParam("firstName", "John")
			.formParam("lastName", "Doe")
			.formParam("email", "john.doe@gmail.com")
			.formParam("password", "john"))
		.pause(12)
		.exec(http("request_2")
			.post("/signIn")
			.headers(postHeaders)
			.formParam("email", "john.doe@gmail.com")
			.formParam("password", "john")
			.formParam("rememberMe", "true"))
		.pause(25)
		.exec(http("request_3")
			.get("/products/123")
			.headers(getHeaders))
		.pause(4)
		.exec(http("request_4")
			.post("/cart/add/123")
			.headers(postHeaders)
			.formParam("amount", "1"))
		.pause(3)
		.exec(http("request_5")
			.get("/products/169")
			.headers(getHeaders))
		.pause(2)
		.exec(http("request_6")
			.post("/cart/add/169")
			.headers(postHeaders)
			.formParam("amount", "1"))
		.pause(1)
		.exec(http("request_7")
			.get("/cart")
			.headers(getHeaders))
		.pause(17)
		.exec(http("request_8")
			.post("/orders/draft")
			.headers(postHeaders)
			.formParam("address.fullName", "John Doe")
			.formParam("address.streetAddress", "Sesame Street 5")
			.formParam("address.zipCode", "30-051")
			.formParam("address.city", "Cracow")
			.formParam("address.country", "UK")
			.formParam("cart.items[0].product", "123")
			.formParam("cart.items[0].amount", "1")
			.formParam("cart.items[1].product", "169")
			.formParam("cart.items[1].amount", "1"))
		.pause(27)
		.exec(http("request_9")
			.post("/orders/7ac55666-f84b-4b03-9f41-a24fbcf63435/confirm")
			.headers(postHeaders))
		.pause(22)
		.exec(http("request_10")
			.post("/payment/2e0acb85-a438-4e79-a374-b2b643656a74/pay")
			.headers(postHeaders)
			.formParam("cc-name", "John Doe")
			.formParam("cc-number", "23423423423423423424")
			.formParam("cc-exp", "20/54")
			.formParam("x_card_code", "546"))
		.pause(3)
		.exec(http("request_11")
			.get("/orders")
			.headers(getHeaders))
		.pause(1)
		.exec(http("request_12")
			.get("/orders/7ac55666-f84b-4b03-9f41-a24fbcf63435")
			.headers(getHeaders))
		.pause(2)
		.exec(http("request_13")
			.get("/products/169")
			.headers(getHeaders))
		.pause(6)
		.exec(http("request_14")
			.post("/products/169/review")
			.headers(postHeaders)
			.formParam("author", "John Doe")
			.formParam("rating", "5")
			.formParam("content", ":))"))

	setUp(buyingScenario.inject(atOnceUsers(1))).protocols(httpProtocol)
}