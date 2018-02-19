package pl.edu.agh.msc.orders

import java.time.LocalDateTime

import pl.edu.agh.msc.cart.{ Cart, CartItem }
import pl.edu.agh.msc.common.IntegrationTest
import pl.edu.agh.msc.pricing.{ Money, PriceRepository }
import pl.edu.agh.msc.products.{ ProductId, ProductRepoView, ProductRepository }
import cats.syntax.option._
import pl.edu.agh.msc.utils.Time
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind

import scala.concurrent.Future

class OrderSpec extends IntegrationTest with ProductFactories {

  class StubTime(t: LocalDateTime) extends Time { def now(): LocalDateTime = t }

  private val ordersService = behaviorOf[OrdersService]

  it should "create order draft with assigned prices" in {
    // GIVEN
    val laptop = createProduct(name  = "Laptop", price = Money(2200)).await()
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
    val id = ordersService.saveDraft(draft).await()
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
  }

  private val currentTime = LocalDateTime.now()

  override def fakeApplication(): Application = new GuiceApplicationBuilder().overrides(bind[Time].toInstance(new StubTime(currentTime))).build()

}

trait ProductFactories { this: IntegrationTest =>

  private val productsRepository = inject[ProductRepository]
  private val pricesRepository = inject[PriceRepository]

  def createProduct(
    name:  String,
    price: Money
  ): Future[ProductId] = {
    for {
      id <- productsRepository.insert(ProductRepoView(
        name,
        price,
        photo               = None,
        cachedAverageRating = None,
        description         = ""
      ))
      _ <- pricesRepository.save(id, price)
    } yield id
  }

}