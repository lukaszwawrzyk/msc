package pl.edu.agh.msc.orders

import java.time.{ LocalDateTime, Month }

import org.scalatest.{ Inside, LoneElement }
import pl.edu.agh.msc.cart.{ Cart, CartItem }
import pl.edu.agh.msc.common.IntegrationTest
import pl.edu.agh.msc.pricing.Money
import pl.edu.agh.msc.products.ProductFactories
import pl.edu.agh.msc.user.UserFactories
import pl.edu.agh.msc.utils.Time
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder

class OrderSpec extends IntegrationTest with Inside with LoneElement with ProductFactories with OrderFactories with UserFactories {

  private val ordersService = behaviorOf[OrdersService]

  it should "create order draft with assigned prices" in {
    // GIVEN
    val john = createAndSaveUser().await()
    val laptop = createAndSaveProduct(name  = "Laptop", price = Money(2200)).await()
    val draft = OrderDraft(
      Cart(Seq(CartItem(laptop, amount = 2))),
      Address(
        "John Doe",
        "Wall St. 123",
        "94523",
        "Washington",
        "United States of America"
      )
    )

    // WHEN
    val id = ordersService.saveDraft(draft, john).await()
    val order = ordersService.find(id).await()

    // THEN
    order.address shouldBe Address(
      "John Doe",
      "Wall St. 123",
      "94523",
      "Washington",
      "United States of America"
    )
    order.status shouldBe OrderStatus.Unconfirmed
    order.items shouldBe Seq(LineItem(laptop, amount = 2, price = Money(2200)))
    order.date shouldBe currentTime
    order.buyer shouldBe john
  }

  it should "confirm order" in {
    // GIVEN
    val john = createAndSaveUser().await()
    val laptop = createAndSaveProduct(name  = "Laptop", price = Money(2200)).await()
    val draft = OrderDraft(
      Cart(Seq(CartItem(laptop, amount = 2))),
      createAddress()
    )

    // WHEN
    val id = ordersService.saveDraft(draft, john).await()
    ordersService.confirm(id).await()
    val order = ordersService.find(id).await()

    // THEN
    order.status shouldBe OrderStatus.Confirmed
    order.items shouldBe Seq(LineItem(laptop, amount = 2, price = Money(2200)))
    order.date shouldBe currentTime
    order.buyer shouldBe john
  }

  it should "return list of historical orders" in {
    // GIVEN
    val john = createAndSaveUser().await()
    val laptop = createAndSaveProduct(name  = "Laptop", price = Money(2200)).await()
    val tablet = createAndSaveProduct(name  = "Tablet", price = Money(800)).await()
    val address = createAddress()

    val laptopOrder = OrderDraft(
      Cart(Seq(CartItem(laptop, amount = 2))),
      address
    )

    val tabletOrder = OrderDraft(
      Cart(Seq(CartItem(tablet, amount = 1))),
      address
    )

    // WHEN
    time.set(march)
    val laptopOrderId = ordersService.saveDraft(laptopOrder, john).await()
    ordersService.confirm(laptopOrderId).await()

    time.set(june)
    val tabletOrderId = ordersService.saveDraft(tabletOrder, john).await()
    ordersService.confirm(tabletOrderId).await()

    val orders = ordersService.historical(john).await()

    // THEN
    inside(orders) {
      case Seq(later, earlier) =>
        later.items.loneElement shouldBe LineItem(tablet, amount = 1, Money(800))
        later.date shouldBe june
        later.id shouldBe tabletOrderId

        earlier.items.loneElement shouldBe LineItem(laptop, amount = 2, Money(2200))
        earlier.date shouldBe march
        earlier.id shouldBe laptopOrderId
    }
  }

  it should "confirm the payment" in {
    // GIVEN
    val john = createAndSaveUser().await()
    val laptop = createAndSaveProduct(name  = "Laptop", price = Money(2200)).await()
    val draft = OrderDraft(
      Cart(Seq(CartItem(laptop, amount = 2))),
      createAddress()
    )

    // WHEN
    val id = ordersService.saveDraft(draft, john).await()
    ordersService.confirm(id).await()
    ordersService.paymentConfirmed(id).await()
    val order = ordersService.find(id).await()

    // THEN
    order.status shouldBe OrderStatus.Paid
  }

  private lazy val currentTime = LocalDateTime.now()
  private val march = LocalDateTime.of(2018, Month.MARCH, 10, 23, 30)
  private val june = march.withMonth(Month.JUNE.getValue)
  private lazy val time = new StubTime(currentTime)

  class StubTime(private var t: LocalDateTime) extends Time {
    def now(): LocalDateTime = t
    def set(v: LocalDateTime): Unit = { t = v }
  }

  override protected def configureApp(builder: GuiceApplicationBuilder): GuiceApplicationBuilder =
    builder.overrides(bind[Time].toInstance(time))

}
