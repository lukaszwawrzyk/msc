package pl.edu.agh.msc.perftests

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class RecordedSimulation extends Simulation {

	private val baseUrl = "http://localhost:9000"

	val headers = Map(
		"Origin" -> baseUrl,
		"Upgrade-Insecure-Requests" -> "1"
	)

	val httpProtocol = http
		.baseURL(baseUrl)
//		.inferHtmlResources()
		.acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
		.acceptEncodingHeader("gzip, deflate")
		.acceptLanguageHeader("pl-PL,pl;q=0.9,en-US;q=0.8,en;q=0.7")
		.userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36 OPR/51.0.2830.55")
  	.headers(headers)


  val random = new Random(lastProductId = 501)

  val userFeeder = Iterator.continually {
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
      "cardCode" -> random.range(100, 999),
      "comment" -> random.fake.lorem.paragraph(3),
      "rating" -> random.range(1, 5)
    )
  }

  def get(name: String, uri: String) = http(name).get(uri)
  def post(name: String, uri: String) = http(name).post(uri)

  val signUp =
     exec(get("show signup form", "/signUp")).exitHereIfFailed
    .exec(post("post signup form", "/signUp")
      .formParam("firstName", "${firstName}")
      .formParam("lastName", "${lastName}")
      .formParam("email", "${email}")
      .formParam("password", "${password}")).exitHereIfFailed

  val signIn = exec(post("post sign in form", "/signIn")
      .formParam("email", "${email}")
      .formParam("password", "${password}")
      .formParam("rememberMe", "true")).exitHereIfFailed

  // todo add pause

  val addProductsToCart = foreach("${productsToBuy}", "productToBuy") {
	  exec(get("show main product", "/products/${productToBuy}"))
	 .exec(post("add main product to cart", "/cart/add/${productToBuy}")
		  .formParam("amount", "1"))
  }

  val placeOrder = exec {
	  get("open cart", "/cart")
  }.exec { session =>
      val products = session("productsToBuy").as[Seq[Int]]
      val formEntries = products.zipWithIndex.flatMap { case (productId, index) =>
        Seq(
          s"cart.items[$index].product" -> productId,
          s"cart.items[$index].amount"  -> 1
        )
      }.toMap
      session.set("orderFormEntries", formEntries)
  }.exec {
    post("create order draft", "/orders/draft")
      .formParam("address.fullName", "${firstName} ${lastName}")
      .formParam("address.streetAddress", "${streetAddress}")
      .formParam("address.zipCode", "${zipCode}")
      .formParam("address.city", "${city}")
      .formParam("address.country", "${country}")
      .formParamMap("${orderFormEntries}")
      .check(css("#order-confirm-form", "action").saveAs("orderConfirmUri"))
  }.exec {
    post("confirm order", "${orderConfirmUri}")
      .check(css("#payment-form", "action").saveAs("paymentConfirmUri"))
  }.exec {
    post("pay", "${paymentConfirmUri}")
      .formParam("cc-name", "${firstName} {lastName}")
      .formParam("cc-number", "${cardNumber}")
      .formParam("cc-exp", "${cardExpiry}")
      .formParam("x_card_code", "${cardCode}")
  }

	val reviewLastBoughtProduct = exec(
		get("show historical orders", "/orders")
			.check(css("#historical-orders > tbody > tr:nth-child(1) > td:nth-child(1) > a", "href").saveAs("lastOrderDetailsUri"))
	).exec(
		get("show last order details", "${lastOrderDetailsUri}")
	  	.check(css("#line-items-table > tbody > tr:first-child > td:nth-child(2) > a", "href").saveAs("boughtProductUrl"))
	).exec(
		get("show last bought product", "${boughtProductUrl}")
	).exec(
		post("post the comment", "${boughtProductUrl}/review")
			.formParam("author", "${firstName} ${lastName}")
			.formParam("rating", "${rating}")
			.formParam("content", "${comment}")
	)

  val buyingScenario = scenario("Buying Scenario")
    .feed(userFeeder)
    .exec(signUp, signIn, addProductsToCart, placeOrder, reviewLastBoughtProduct)

	setUp(buyingScenario.inject(atOnceUsers(100))).protocols(httpProtocol)
}