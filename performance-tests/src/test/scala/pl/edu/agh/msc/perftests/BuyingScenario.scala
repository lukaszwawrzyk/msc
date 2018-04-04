package pl.edu.agh.msc.perftests

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

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
      "shouldReview" -> random.boolean(probability = 0.2),
      "comment" -> random.fake.lorem.paragraph(3),
      "rating" -> random.range(1, 5)
    )
  }


  val signUp =
    exec(get("show signup form", "/signUp")).pause(10, 30).exitHereIfFailed
      .exec(post("post signup form", "/signUp")
        .formParam("firstName", "${firstName}")
        .formParam("lastName", "${lastName}")
        .formParam("email", "${email}")
        .formParam("password", "${password}")).exitHereIfFailed

  val signIn = pause(10, 20)
    .exec(post("post sign in form", "/signIn")
      .formParam("email", "${email}")
      .formParam("password", "${password}")
      .formParam("rememberMe", "true")).exitHereIfFailed


  val addProductsToCart = tryMax(3) {
    foreach("${productsToBuy}", "productToBuy") {
      pause(3, 2.minutes)
        .exec(get("show product", "/products/${productToBuy}")).pause(3, 3.minutes)
        .exec(post("add to cart", "/cart/add/${productToBuy}")
          .formParam("amount", "1")).pause(1, 10)
    }
  }.exitHereIfFailed

  val placeOrder = exec {
    get("open cart", "/cart")
  }.pause(30, 2.minutes).exec { session =>
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
  }.exitHereIfFailed.pause(5, 2.minutes).exec {
    post("confirm order", "${orderConfirmUri}")
      .check(css("#payment-form", "action").saveAs("paymentConfirmUri"))
  }.exitHereIfFailed.pause(1.minute, 5.minutes).exec {
    post("pay", "${paymentConfirmUri}")
      .formParam("cc-name", "${firstName} {lastName}")
      .formParam("cc-number", "${cardNumber}")
      .formParam("cc-exp", "${cardExpiry}")
      .formParam("x_card_code", "${cardCode}")
  }.exitHereIfFailed.pause(1.minute)

  // normally this would be a separate scenario, but it is easier to do it together
  // as we have access to user accounts that are generated automatically.
  // On average this should give simialr results.
  val reviewLastBoughtProduct = doIf("${shouldReview}") {
    exec(
      get("show historical orders", "/orders")
        .check(css("#historical-orders > tbody > tr:nth-child(1) > td:nth-child(1) > a", "href").saveAs("lastOrderDetailsUri"))
    ).exitHereIfFailed.pause(5, 30).exec(
      get("show last order details", "${lastOrderDetailsUri}")
        .check(css("#line-items-table > tbody > tr:first-child > td:nth-child(2) > a", "href").saveAs("boughtProductUrl"))
    ).exitHereIfFailed.pause(5, 30).exec(
      get("show last bought product", "${boughtProductUrl}")
    ).exitHereIfFailed.pause(30, 5.minutes).exec(
      post("post the comment", "${boughtProductUrl}/review")
        .formParam("author", "${firstName} ${lastName}")
        .formParam("rating", "${rating}")
        .formParam("content", "${comment}")
    )
  }

  def repeating = scenario("Buying Scenario")
    .feed(feeder)
    .exec(signUp, signIn)
    .forever(exec(addProductsToCart, placeOrder, reviewLastBoughtProduct))

  def single = scenario("Buying Scenario")
    .feed(feeder)
    .exec(signUp, signIn, addProductsToCart, placeOrder, reviewLastBoughtProduct)

}