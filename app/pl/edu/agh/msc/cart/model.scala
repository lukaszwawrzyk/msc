package pl.edu.agh.msc.cart

import pl.edu.agh.msc.products.ProductId

case class Cart(
  items: Seq[CartItem]
)

case class CartItem(
  product: ProductId,
  amount:  Int
)