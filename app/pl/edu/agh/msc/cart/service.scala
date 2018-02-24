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
  )(implicit ec: ExecutionContext): Future[Unit] = {
    cartRepository.insert(user, CartItem(product, amount))
  }

  def get(user: UUID)(implicit ec: ExecutionContext): Future[Cart] = {
    cartRepository.find(user).map(Cart(_))
  }

  def clear(user: UUID)(implicit ec: ExecutionContext): Future[Unit] = {
    cartRepository.delete(user)
  }

}
