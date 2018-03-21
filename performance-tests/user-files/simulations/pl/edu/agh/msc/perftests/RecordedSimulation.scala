package pl.edu.agh.msc.perftests

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class RecordedSimulation extends Simulation {

	val httpProtocol = http
		.baseURL("http://localhost:9000")
		.inferHtmlResources()
		.acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
		.acceptEncodingHeader("gzip, deflate")
		.acceptLanguageHeader("pl-PL,pl;q=0.9,en-US;q=0.8,en;q=0.7")
		.userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36 OPR/51.0.2830.55")

	val headers_0 = Map("Upgrade-Insecure-Requests" -> "1")

	val headers_1 = Map(
		"Origin" -> "http://localhost:9000",
		"Upgrade-Insecure-Requests" -> "1")

    val uri2 = "https://code.jquery.com/jquery-3.2.1.slim.min.js"
    val uri3 = "cdnjs.cloudflare.com"
    val uri4 = "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0"
    val uri5 = "http://fonts.googleapis.com/css"

	val scn = scenario("RecordedSimulation")
		.exec(http("request_0")
			.get("/signUp")
			.headers(headers_0))
		.pause(15)
		.exec(http("request_1")
			.post("/signUp")
			.headers(headers_1)
			.formParam("firstName", "John")
			.formParam("lastName", "Doe")
			.formParam("email", "john.doe@gmail.com")
			.formParam("password", "john"))
		.pause(12)
		.exec(http("request_2")
			.post("/signIn")
			.headers(headers_1)
			.formParam("email", "john.doe@gmail.com")
			.formParam("password", "john")
			.formParam("rememberMe", "true"))
		.pause(25)
		.exec(http("request_3")
			.get("/products/123")
			.headers(headers_0))
		.pause(4)
		.exec(http("request_4")
			.post("/cart/add/123")
			.headers(headers_1)
			.formParam("amount", "1"))
		.pause(3)
		.exec(http("request_5")
			.get("/products/169")
			.headers(headers_0))
		.pause(2)
		.exec(http("request_6")
			.post("/cart/add/169")
			.headers(headers_1)
			.formParam("amount", "1"))
		.pause(1)
		.exec(http("request_7")
			.get("/cart")
			.headers(headers_0))
		.pause(17)
		.exec(http("request_8")
			.post("/orders/draft")
			.headers(headers_1)
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
			.headers(headers_1))
		.pause(22)
		.exec(http("request_10")
			.post("/payment/2e0acb85-a438-4e79-a374-b2b643656a74/pay")
			.headers(headers_1)
			.formParam("cc-name", "John Doe")
			.formParam("cc-number", "23423423423423423424")
			.formParam("cc-exp", "20/54")
			.formParam("x_card_code", "546"))
		.pause(3)
		.exec(http("request_11")
			.get("/orders")
			.headers(headers_0))
		.pause(1)
		.exec(http("request_12")
			.get("/orders/7ac55666-f84b-4b03-9f41-a24fbcf63435")
			.headers(headers_0))
		.pause(2)
		.exec(http("request_13")
			.get("/products/169")
			.headers(headers_0))
		.pause(6)
		.exec(http("request_14")
			.post("/products/169/review")
			.headers(headers_1)
			.formParam("author", "John Doe")
			.formParam("rating", "5")
			.formParam("content", ":))"))

	setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}