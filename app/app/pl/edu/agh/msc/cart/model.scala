package pl.edu.agh.msc.cart

import pl.edu.agh.msc.products.ProductId

case class Cart(
  items: Seq[CartItem]
)

object Cart {
  val Empty = Cart(Seq.empty)
}

case class CartItem(
  product: ProductId,
  amount:  Int
)