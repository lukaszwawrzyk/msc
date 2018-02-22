package pl.edu.agh.msc.products

import pl.edu.agh.msc.common.IntegrationTest
import pl.edu.agh.msc.pricing.{ Money, PriceRepository }

import scala.concurrent.Future

trait ProductFactories { this: IntegrationTest =>

  private val productsRepository = inject[ProductRepository]
  private val pricesRepository = inject[PriceRepository]

  def createProduct(
    name:  String,
    price: Money
  ): Future[ProductId] = {
    for {
      id <- productsRepository.insert(ProductRepoView(
        name,
        price,
        photo               = None,
        cachedAverageRating = None,
        description         = ""
      ))
      _ <- pricesRepository.save(id, price)
    } yield id
  }

}
