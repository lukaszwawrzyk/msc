package pl.edu.agh.msc.perftests

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class BuyingScenario(random: Random) {

  val feeder = Iterator.continually {
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


  val addProductsToCart = foreach("${productsToBuy}", "productToBuy") {
    exec(get("show product", "/products/${productToBuy}"))
      .exec(post("add to cart", "/cart/add/${productToBuy}")
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

  def create = scenario("Buying Scenario")
    .feed(feeder)
    .exec(signUp, signIn, addProductsToCart, placeOrder, reviewLastBoughtProduct)

}