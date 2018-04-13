package pl.edu.agh.msc.perftests

import io.gatling.core.Predef._
import scala.concurrent.duration._
import CommonActions._

class BrowsingScenario(random: Random) {

	val feeder = Iterator.continually(Map(
		"productsToViewPerSearch" -> Seq.fill(random.range(1, 5))(Seq.fill(random.range(1, 8))(random.productId()))
	))

	val landingPage = exec(get("Landing page", "/landing")).pause(10, 60)

	val openSearchPage = exec(get("Products no query", "/products")).pause(10, 30)

	def searchProducts(name: String) = exec(get("Products by name", s"/products?text=$name"))

	val searchStrings = Seq("Small", "Ergonomic", "Rustic", "Intelligent", "Gorgeous", "Incredible", "Fantastic", "Practical", "Sleek", "Awesome", "Enormous", "Mediocre", "Synergistic", "Heavy Duty", "Lightweight", "Aerodynamic", "Durable", "Steel", "Wooden", "Concrete", "Plastic", "Cotton", "Granite", "Rubber", "Leather", "Silk", "Wool", "Linen", "Marble", "Iron", "Bronze", "Copper", "Aluminum", "Paper", "Chair", "Car", "Computer", "Gloves", "Pants", "Shirt", "Table", "Shoes", "Hat", "Plate", "Knife", "Bottle", "Coat", "Lamp", "Keyboard", "Bag", "Bench", "Clock", "Watch", "Wallet")

	val browse = foreach("${productsToViewPerSearch}", "productsToView") {
		uniformRandomSwitch(
			searchStrings.map(name => exec(
				searchProducts(name).pause(10, 30),
				foreach("${productsToView}", "productToView") {
					productDetails("${productToView}").pause(10, 3.minutes)
				}
			)): _*
		)
	}


	def repeating = {
		scenario("Browsing Scenario")
			.feed(feeder)
			.forever(exec(landingPage, openSearchPage, browse))
	}

	def single = {
		scenario("Browsing Scenario")
			.feed(feeder)
			.exec(landingPage, openSearchPage, browse)
	}

}