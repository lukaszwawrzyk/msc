package pl.edu.agh.msc.orders

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{ Inject, Singleton }
import java.util.concurrent.Future

import pl.edu.agh.msc.products.ProductId
import pl.edu.agh.msc.utils.SlickTypeMappings
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

@Singleton class OrdersRepository @Inject() (dbConfigProvider: DatabaseConfigProvider) extends SlickTypeMappings {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  protected val profile = dbConfig.profile
  import dbConfig.db
  import profile.api._

  private case class OrderRow(
    status:        Int,
    date:          LocalDateTime,
    fullName:      String,
    streetAddress: String,
    zipCode:       String,
    city:          String,
    country:       String,
    id:            UUID
  )

  private case class LineItemRow(
    orderId:   UUID,
    productId: Long,
    amount:    Int,
    price:     BigDecimal
  )

  def insert(order: Order)(implicit ec: ExecutionContext): Future[Unit] = {
    ???
  }

}
