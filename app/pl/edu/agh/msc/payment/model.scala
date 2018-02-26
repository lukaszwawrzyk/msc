package pl.edu.agh.msc.payment

import java.net.URL
import java.util.UUID

import pl.edu.agh.msc.orders.Address
import pl.edu.agh.msc.pricing.Money

case class PaymentId(value: UUID) extends AnyVal

case class PaymentRequest(
  totalPrice: Money,
  email: String,
  buyer: Address,
  products: Seq[Product],
  returnUrl: URL
)

case class Product(name: String, unitPrice: Money, amount: Int)
