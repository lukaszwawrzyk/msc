package pl.edu.agh.msc.recommendation

import pl.edu.agh.msc.common.IntegrationTest
import pl.edu.agh.msc.pricing.Money
import pl.edu.agh.msc.products.ProductFactories
import pl.edu.agh.msc.user.UserFactories

class RecommendationSpec extends IntegrationTest with UserFactories with ProductFactories {

  private val recommendationService = behaviorOf[RecommendationService]

  it should "return something for the user without orders" in {
    // GIVEN
    createAndSaveProduct(name   = "Prod 1", price = Money(2000), rating = 3).await()
    createAndSaveProduct(name   = "Prod 2", price = Money(5000), rating = 5).await()
    createAndSaveProduct(name   = "Prod 3", price = Money(5000), rating = 5).await()
    createAndSaveProduct(name   = "Prod 4", price = Money(5000), rating = 5).await()
    createAndSaveProduct(name   = "Prod 5", price = Money(5000), rating = 5).await()
    createAndSaveProduct(name   = "Prod 6", price = Money(5000), rating = 5).await()
    val newUser = createAndSaveUser().await()

    // WHEN
    val products = recommendationService.forUser(newUser).await()

    // THEN
    products should have size 5
  }

}
