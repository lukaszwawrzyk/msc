package pl.edu.agh.msc.orders

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{ Inject, Singleton }

import pl.edu.agh.msc.pricing.Money
import pl.edu.agh.msc.products.ProductId
import pl.edu.agh.msc.utils.SlickTypeMappings
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class OrdersRepository @Inject() (dbConfigProvider: DatabaseConfigProvider) extends SlickTypeMappings {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  protected val profile = dbConfig.profile
  import dbConfig.db
  import profile.api._

  private case class OrderRow(
    status:        Int,
    buyerId:       UUID,
    date:          LocalDateTime,
    fullName:      String,
    streetAddress: String,
    zipCode:       String,
    city:          String,
    country:       String,
    id:            UUID
  )

  private class Orders(tag: Tag) extends Table[OrderRow](tag, "orders") {
    def status = column[Int]("status")
    def userId = column[UUID]("user_id")
    def date = column[LocalDateTime]("date")
    def fullName = column[String]("full_name")
    def streetAddress = column[String]("street_address")
    def zipCode = column[String]("zip_code")
    def city = column[String]("city")
    def country = column[String]("country")
    def id = column[UUID]("id", O.PrimaryKey)
    def * = (status, userId, date, fullName, streetAddress, zipCode, city, country, id).mapTo[OrderRow]
  }

  private case class LineItemRow(
    orderId:   UUID,
    productId: Long,
    amount:    Int,
    price:     BigDecimal,
    id:        Long       = 0L
  )

  private class LineItems(tag: Tag) extends Table[LineItemRow](tag, "line_items") {
    def orderId = column[UUID]("order_id")
    def productId = column[Long]("product_id")
    def amount = column[Int]("amount")
    def price = column[BigDecimal]("price")
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def * = (orderId, productId, amount, price, id).mapTo[LineItemRow]
  }

  private val baseOrderQuery = TableQuery[Orders]
  private val baseLineItemsQuery = TableQuery[LineItems]
  private val orderByIdQuery = Compiled { id: Rep[UUID] =>
    baseOrderQuery.filter(_.id === id)
  }
  private val orderStatusByIdQuery = Compiled { id: Rep[UUID] =>
    baseOrderQuery.filter(_.id === id).map(_.status)
  }
  private val orderByUserQuery = Compiled { userId: Rep[UUID] =>
    baseOrderQuery.filter(_.userId === userId).sortBy(_.date.desc)
  }
  private val lineItemsByOrderQuery = Compiled { orderId: Rep[UUID] =>
    baseLineItemsQuery.filter(_.orderId === orderId)
  }

  def find(id: OrderId)(implicit ec: ExecutionContext): Future[Order] = db.run {
    orderByIdQuery(id.value).result.head.flatMap(convertRow)
  }

  def findByUser(user: UUID)(implicit ec: ExecutionContext): Future[Seq[Order]] = db.run {
    for {
      orderRows <- orderByUserQuery(user).result
      orders <- DBIO.sequence(orderRows.map(convertRow))
    } yield orders
  }

  def changeStatus(id: OrderId, status: OrderStatus.Value)(implicit ec: ExecutionContext): Future[Unit] = db.run {
    orderStatusByIdQuery(id.value).update(status.id).map(_ => ())
  }

  def insert(order: Order)(implicit ec: ExecutionContext): Future[Unit] = db.run {
    val orderRow = OrderRow(
      order.status.id,
      order.buyer,
      order.date,
      order.address.fullName,
      order.address.streetAddress,
      order.address.zipCode,
      order.address.city,
      order.address.country,
      order.id.value
    )
    val lineItemRows = order.items.map { item =>
      LineItemRow(order.id.value, item.product.value, item.amount, item.price.value)
    }

    (baseOrderQuery += orderRow) >>
      DBIO.sequence(lineItemRows.map(baseLineItemsQuery += _)) >>
      DBIO.successful(())
  }

  private def convertRow(orderRow: OrderRow)(implicit ec: ExecutionContext): DBIO[Order] = {
    for {
      lineItemRows: Seq[LineItemRow] <- lineItemsByOrderQuery(orderRow.id).result
    } yield {
      val address = Address(
        orderRow.fullName,
        orderRow.streetAddress,
        orderRow.zipCode,
        orderRow.city,
        orderRow.country
      )

      val lineItems = lineItemRows.map { itemRow =>
        LineItem(ProductId(itemRow.productId), itemRow.amount, Money(itemRow.price))
      }

      Order(
        OrderId(orderRow.id),
        orderRow.buyerId,
        address,
        OrderStatus(orderRow.status),
        lineItems,
        orderRow.date
      )
    }
  }

}
