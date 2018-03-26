package pl.edu.agh.msc.pricing

import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.products.ProductId

import scala.util.Try

@Singleton class PricingService @Inject() (priceRepository: PriceRepository) {

  def find(id: ProductId): Option[Money] = {
    Try(priceRepository.find(id)).toOption
  }

}
