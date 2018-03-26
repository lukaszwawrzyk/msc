package pl.edu.agh.msc.cart

import java.util.UUID

import pl.edu.agh.msc.products.ProductId
import javax.inject.{ Singleton, Inject }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton class CartService @Inject() (
  cartRepository: CartRepository
) {

  def add(
    user:    UUID,
    product: ProductId,
    amount:  Int
  ): Unit = {
    cartRepository.insert(user, CartItem(product, amount))
  }

  def get(user: UUID): Cart = {
    Cart(cartRepository.find(user))
  }

  def clear(user: UUID): Unit = {
    cartRepository.delete(user)
  }

}
