package pl.edu.agh.msc.perftests

import io.gatling.core.Predef._

class BrowsingScenario(random: Random) {

	val feeder = Iterator.continually(Map(
		"productsToViewPerSearch" -> Seq.fill(random.range(1, 5))(Seq.fill(random.range(1, 8))(random.productId()))
	))

	val landingPage = exec(get("Landing page", "/landing"))

	val openSearchPage = exec(get("Products no query", "/products"))

	def searchProducts(name: String) = exec(get("Products by name", s"/products?text=$name"))

	def productDetails(id: String) = exec(get("Products details", s"/products/$id"))

	val searchStrings = Seq("Small", "Ergonomic", "Rustic", "Intelligent", "Gorgeous", "Incredible", "Fantastic", "Practical", "Sleek", "Awesome", "Enormous", "Mediocre", "Synergistic", "Heavy Duty", "Lightweight", "Aerodynamic", "Durable", "Steel", "Wooden", "Concrete", "Plastic", "Cotton", "Granite", "Rubber", "Leather", "Silk", "Wool", "Linen", "Marble", "Iron", "Bronze", "Copper", "Aluminum", "Paper", "Chair", "Car", "Computer", "Gloves", "Pants", "Shirt", "Table", "Shoes", "Hat", "Plate", "Knife", "Bottle", "Coat", "Lamp", "Keyboard", "Bag", "Bench", "Clock", "Watch", "Wallet")

	val browse = foreach("${productsToViewPerSearch}", "productsToView") {
		uniformRandomSwitch(
			searchStrings.map(name => exec(
				searchProducts(name),
				foreach("${productsToView}", "productToView") {
					productDetails("${productToView}")
				}
			)): _*
		)
	}


	def create = {
		scenario("Browsing Scenario")
			.feed(feeder)
			.exec(landingPage, openSearchPage, browse)
	}
}