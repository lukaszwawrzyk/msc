package pl.edu.agh.msc.availability

import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.products.ProductId

@Singleton class AvailabilityService @Inject() (availabilityRepository: AvailabilityRepository) {

  def find(id: ProductId): Option[Availability] = {
    availabilityRepository.find(id)
  }

}
