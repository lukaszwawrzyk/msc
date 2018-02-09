package pl.agh.edu.msc.availability

import javax.inject.{ Inject, Singleton }

import pl.agh.edu.msc.products.ProductId

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class AvailabilityService @Inject() (availabilityRepository: AvailabilityRepository) {

  def find(id: ProductId)(implicit ec: ExecutionContext): Future[Option[Availability]] = {
    availabilityRepository.find(id)
  }

}
