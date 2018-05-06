package pl.edu.agh.msc.payment.read

import java.net.URL
import java.util.UUID

import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.orders.Address
import pl.edu.agh.msc.payment.{ PaymentId, PaymentRequest, Product }
import pl.edu.agh.msc.pricing.Money
import pl.edu.agh.msc.utils.SlickTypeMappings
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class PaymentRepository @Inject() (dbConfigProvider: DatabaseConfigProvider) extends SlickTypeMappings {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  protected val profile = dbConfig.profile
  import dbConfig.db
  import profile.api._

  private case class PaymentRow(
    totalPrice: BigDecimal,
    email:      String,
    address:    String,
    returnUrl:  String,
    isPaid:     Boolean,
    id:         UUID
  )

  private class Payments(tag: Tag) extends Table[PaymentRow](tag, "payments") {
    def totalPrice = column[BigDecimal]("total_price")
    def email = column[String]("email")
    def address = column[String]("address")
    def returnUrl = column[String]("return_url")
    def isPaid = column[Boolean]("is_paid")
    def id = column[UUID]("id", O.PrimaryKey)
    def * = (totalPrice, email, address, returnUrl, isPaid, id).mapTo[PaymentRow]
  }

  private case class ProductRow(
    name:      String,
    unitPrice: BigDecimal,
    amount:    Int,
    paymentId: UUID,
    id:        Long       = 0L
  )

  private class Products(tag: Tag) extends Table[ProductRow](tag, "payment_products") {
    def name = column[String]("name")
    def unitPrice = column[BigDecimal]("unit_price")
    def amount = column[Int]("amount")
    def paymentId = column[UUID]("payment_id")
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def * = (name, unitPrice, amount, paymentId, id).mapTo[ProductRow]
  }

  private val basePaymentQuery = TableQuery[Payments]
  private val baseProductsQuery = TableQuery[Products]
  private val paymentByIdQuery = Compiled { id: Rep[UUID] =>
    basePaymentQuery.filter(_.id === id)
  }
  private val paymentStatusByIdQuery = Compiled { id: Rep[UUID] =>
    basePaymentQuery.filter(_.id === id).map(_.isPaid)
  }
  private val productsByPaymentQuery = Compiled { paymentId: Rep[UUID] =>
    baseProductsQuery.filter(_.paymentId === paymentId)
  }

  def insert(id: PaymentId, payment: PaymentRequest)(implicit ec: ExecutionContext): Future[Unit] = db.run {
    val address = payment.address.productIterator.asInstanceOf[Iterator[String]].mkString(";")
    val paymentRow = PaymentRow(
      payment.totalPrice.value,
      payment.email,
      address,
      payment.returnUrl.toString,
      isPaid = false,
      id.value
    )
    val productRows = payment.products.map { product =>
      ProductRow(product.name, product.unitPrice.value, product.amount, id.value)
    }

    DBIO.seq(
      basePaymentQuery += paymentRow,
      baseProductsQuery ++= productRows
    )
  }

  def find(id: PaymentId)(implicit ec: ExecutionContext): Future[PaymentRequest] = db.run {
    for {
      paymentRow: PaymentRow <- paymentByIdQuery(id.value).result.head
      productRows: Seq[ProductRow] <- productsByPaymentQuery(id.value).result
    } yield {
      val address = paymentRow.address.split(";").toSeq match {
        case Seq(fullName, streetAddress, zipCode, city, country) =>
          Address(fullName, streetAddress, zipCode, city, country)
      }
      val products = productRows.map { row =>
        Product(row.name, Money(row.unitPrice), row.amount)
      }
      PaymentRequest(
        Money(paymentRow.totalPrice),
        paymentRow.email,
        address,
        products,
        new URL(paymentRow.returnUrl)
      )
    }
  }

  def getPaymentStatus(id: PaymentId)(implicit ec: ExecutionContext): Future[Boolean] = db.run {
    paymentStatusByIdQuery(id.value).result.head
  }

  def setPaymentStatus(id: PaymentId, isPaid: Boolean)(implicit ec: ExecutionContext): Future[Unit] = db.run {
    DBIO.seq(paymentStatusByIdQuery(id.value).update(isPaid))
  }

}
