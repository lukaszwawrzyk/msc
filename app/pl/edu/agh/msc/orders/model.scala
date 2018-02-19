package pl.edu.agh.msc.orders

import java.time.LocalDateTime
import java.util.UUID

import pl.edu.agh.msc.cart.Cart
import pl.edu.agh.msc.pricing.Money
import pl.edu.agh.msc.products.ProductId

case class OrderId(value: UUID) extends AnyVal

case class OrderDraft(
  cart:    Cart,
  address: Address
)

case class Order(
  id:      OrderId,
  address: Address,
  status:  OrderStatus.Value,
  items:   Seq[LineItem],
  date:    LocalDateTime
)

object OrderStatus extends Enumeration {
  val Unconfirmed, Confirmed, Paid = Value
}

case class LineItem(
  product: ProductId,
  amount:  Int,
  price:   Money
)

case class Address(
  fullName:      String,
  streetAddress: String,
  zipCode:       String,
  city:          String,
  country:       String
)