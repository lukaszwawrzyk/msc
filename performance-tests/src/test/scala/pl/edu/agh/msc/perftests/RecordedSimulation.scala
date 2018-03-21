package pl.edu.agh.msc.perftests

import java.util.concurrent.ThreadLocalRandom

import com.github.javafaker.Faker

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
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

  class Random {
    private val jrandom = new ThreadLocalRandom(System.currentTimeMillis())
    private val lastProductId = 1000
    private val random = new scala.util.Random(jrandom)
    val fake = new Faker(jrandom)
    def range(from: Int, to: Int): Int = fake.random.nextInt(to - from) + from
    def productId() = range(1, lastProductId)
    def gaussian() = random.nextGaussian()
  }

  val random = new Random

  val userFeeder = Iterator.continually(
    Map(
      "email" -> random.fake.internet.emailAddress(),
      "password" -> random.fake.internet.password(),
      "firstName" -> random.fake.name.firstName(),
      "lastName" -> random.fake.name.lastName(),
      "productsToBuy" -> Seq.fill(1 + math.max(0, random.gaussian()).toInt)(random.productId()),
      "streetAddress" -> random.fake.address.streetAddress(),
      "zipCode" -> random.fake.address.zipCode(),
      "city" -> random.fake.address.city(),
      "country" -> random.fake.address.country(),
      "cardNumber" -> random.fake.business.creditCardNumber(),
      "cardExpiry" -> random.fake.business.creditCardExpiry(),
      "cardExpiry" -> random.range(100, 999)
    )
  )

  def get(name: String, uri: String) = http(name).get(uri).headers(getHeaders)
  def post(name: String, uri: String) = http(name).post(uri).headers(postHeaders)

  val signUp =
     exec(get("show signup form", "/signUp"))
    .exec(get("post signup form", "/signUp")
      .formParam("firstName", "${firstName}")
      .formParam("lastName", "${lastName}")
      .formParam("email", "${email}")
      .formParam("password", "${password}"))

  val signIn = exec(post("post sign in form", "/signIn")
      .headers(postHeaders)
      .formParam("email", "${email}")
      .formParam("password", "${password}")
      .formParam("rememberMe", "true"))

  def addToCart(productSessionKey: String) = {
    exec(get("show main product", s"/products/$${$productSessionKey}"))
   .exec(post("add main product to cart", s"/cart/add/$${$productSessionKey}")
      .formParam("amount", "1"))
  }

  // todo add pause

  val addProductsToCart = foreach("productsToBuy", "productToBuy")(addToCart("productToBuy"))

  val placeOrder = exec(get("open cart", "/cart"))
    .exec { session =>
      val products = session("productsToBuy").as[Seq[Int]]
      val formEntries = products.zipWithIndex.flatMap { case (productId, index) =>
        Seq(
          s"cart.items[$index].product" -> productId,
          s"cart.items[$index].amount"  -> 1
        )
      }.toMap
      session.set("orderFormEntries", formEntries)
    }
    .exec {
      post("create order draft", "/orders/draft")
        .formParam("address.fullName", "{firstName} {lastName}")
        .formParam("address.streetAddress", "${streetAddress}")
        .formParam("address.zipCode", "{zipCode}")
        .formParam("address.city", "{city}")
        .formParam("address.country", "{country}")
        .formParamMap("${orderFormEntries}")
        .check(css("#order-confirm-form", "action").saveAs("orderConfirmUri"))
    }.exec {
      post("confirm order", "${orderConfirmUri}")
        .check(css("#payment-form", "action").saveAs("paymentConfirmUri"))
    }.exec {
      post("pay", "${paymentConfirmUri}")
        .formParam("cc-name", "{firstName} {lastName}")
        .formParam("cc-number", "${cardNumber}")
        .formParam("cc-exp", "${cardExpiry}")
        .formParam("x_card_code", "${cardCode}")
    }

  val buyingScenario = scenario("Buying Scenario")
    .feed(userFeeder)
    .exec(signUp, signIn, addProductsToCart)
		.exec(http("request_7")
			.get("/cart")
			.headers(getHeaders))
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