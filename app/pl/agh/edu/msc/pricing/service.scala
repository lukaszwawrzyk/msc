package pl.agh.edu.msc.pricing

import javax.inject.{ Inject, Singleton }

import cats.syntax.option._
import pl.agh.edu.msc.products.ProductId

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

@Singleton class PricingService @Inject() (priceRepository: PriceRepository) {

  def find(id: ProductId)(implicit ec: ExecutionContext): Future[Option[Money]] = {
    priceRepository.find(id).map(_.some).recover { case NonFatal(_) => None }
  }

}
