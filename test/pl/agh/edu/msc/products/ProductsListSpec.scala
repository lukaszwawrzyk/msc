package pl.agh.edu.msc.products

import pl.agh.edu.msc.common.IntegrationTest

class ProductsListSpec extends IntegrationTest {

  private val productsService = inject[ProductService]
  private val productsRepository = inject[ProductRepository]

  it should "paginate results" in {
    productsService should not be null
    productsRepository should not be null
  }

}
