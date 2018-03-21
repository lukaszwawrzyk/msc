package pl.edu.agh.msc.orders

import pl.edu.agh.msc.common.IntegrationTest

trait OrderFactories { this: IntegrationTest =>

  def createAddress(): Address = {
    Address(
      "John Doe",
      "Wall St. 123",
      "94523",
      "Washington",
      "United States of America"
    )
  }

}