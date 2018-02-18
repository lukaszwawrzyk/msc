package pl.edu.agh.msc.orders

import java.util.UUID

import pl.edu.agh.msc.cart.Cart

case class OrderId(value: UUID) extends AnyVal

case class Order(
  id: OrderId,
  cart: Cart,
  address: Address
)

case class Address(
  fullName: String,
  streetAddress: String,
  zipCode: String,
  city: String,
  country: String
)