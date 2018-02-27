package pl.edu.agh.msc.payment

import java.net.URL

import pl.edu.agh.msc.common.IntegrationTest
import pl.edu.agh.msc.orders.OrderFactories
import pl.edu.agh.msc.pricing.Money
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.Future

class PaymentSpec extends IntegrationTest with OrderFactories {

  private val paymentService = behaviorOf[PaymentService]

  it should "pass scenario from creating to paying" in {
    // GIVEN
    val paymentRequest = PaymentRequest(
      totalPrice = Money(10000),
      email      = "john.doe@mail.com",
      address      = createAddress(),
      products   = Seq(Product("a smartphone", unitPrice = Money(1000), amount = 10)),
      returnUrl  = new URL("http://msc.com/order/123/payment/confirm")
    )

    // WHEN
    val id = paymentService.create(paymentRequest).await()
    val storedRequest = paymentService.get(id).await()
    val isUnpaidBeforePaying = paymentService.isPaid(id).await()
    paymentService.pay(id).await()
    val isPaidAfterPaying = paymentService.isPaid(id)

    // THEN
    storedRequest shouldEqual paymentRequest
    isUnpaidBeforePaying shouldBe false
    isPaidAfterPaying shouldBe true
    mockNotificationService.expectOneNotification(new URL("http://msc.com/order/123/payment/confirm"))
  }

  lazy val mockNotificationService = new MockNotificationService

  class MockNotificationService extends NotificationService {
    private var calls = List.empty[URL]
    override def notifyURL(url: URL): Future[Unit] = {
      calls :+= url
      Future.unit
    }

    def expectOneNotification(url: URL): Unit = {
      require(List(url) == calls, s"Expected one call to $url, isntead received $calls")
    }
  }

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder().overrides(bind[NotificationService].toInstance(mockNotificationService)).build()
  }

}
