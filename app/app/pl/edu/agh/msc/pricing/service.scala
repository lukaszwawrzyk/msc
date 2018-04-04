package pl.edu.agh.msc.pricing

import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.products.ProductId

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class PricingService @Inject() (priceRepository: PriceRepository) {

  def find(id: ProductId)(implicit ec: ExecutionContext): Future[Option[Money]] = {
    priceRepository.find(id)
  }

}
