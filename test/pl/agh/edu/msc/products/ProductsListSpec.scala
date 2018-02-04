package pl.agh.edu.msc.products

import org.scalatest._
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class ProductsListSpec extends FlatSpec with Matchers with GuiceOneAppPerSuite {

  private val productsService = app.injector.instanceOf[ProductService]
  private val productsRepository = app.injector.instanceOf[ProductRepository]

  it should "paginate results" in {
    productsService should not be null
    productsRepository should not be null
  }

}
