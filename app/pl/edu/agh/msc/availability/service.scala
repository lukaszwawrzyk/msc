package pl.edu.agh.msc.availability

import javax.inject.{ Inject, Singleton }

import pl.edu.agh.msc.products.ProductId

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class AvailabilityService @Inject() (availabilityRepository: AvailabilityRepository) {

  def find(id: ProductId)(implicit ec: ExecutionContext): Future[Option[Availability]] = {
    availabilityRepository.find(id)
  }

}
