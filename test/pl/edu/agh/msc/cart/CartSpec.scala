package pl.edu.agh.msc.cart

import pl.edu.agh.msc.common.IntegrationTest
import pl.edu.agh.msc.orders.{ LineItem, OrderDraft, OrderFactories, OrdersService }
import pl.edu.agh.msc.pricing.Money
import pl.edu.agh.msc.products.ProductFactories
import pl.edu.agh.msc.user.UserFactories

class CartSpec extends IntegrationTest with ProductFactories with UserFactories with OrderFactories {

  private val cartService = behaviorOf[CartService]

  private val ordersService = withHelpOf[OrdersService]

  it should "handle the default order scenario" in {
    // GIVEN
    val client = createAndSaveUser().await()
    val kettle = createProduct("Kettle", Money(10)).await()
    val pot = createProduct("Pot", Money(15)).await()

    // WHEN
    cartService.add(client, kettle, amount = 1).await()
    cartService.add(client, pot, amount = 3).await()
    val cart = cartService.get(client).await()
    val orderDraft = OrderDraft(cart, createAddress())
    val orderId = ordersService.saveDraft(orderDraft, client).await()
    val order = ordersService.find(orderId).await()
    val cartAfterConfirming = cartService.get(client).await()

    // THEN
    cart shouldBe Cart(items = Seq(CartItem(kettle, amount = 1), CartItem(pot, amount = 3)))
    order.items should contain theSameElementsAs Seq(LineItem(kettle, amount = 1, Money(10)), LineItem(pot, amount = 3, Money(15)))
    cartAfterConfirming shouldBe Cart.Empty
  }

}
