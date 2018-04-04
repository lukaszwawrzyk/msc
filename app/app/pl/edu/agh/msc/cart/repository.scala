package pl.edu.agh.msc.cart

import java.util.UUID

import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.products.ProductId
import pl.edu.agh.msc.utils.{ GuardedCall, SlickTypeMappings }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class CartRepository @Inject() (dbConfigProvider: DatabaseConfigProvider, guardedCall: GuardedCall) extends SlickTypeMappings {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  protected val profile = dbConfig.profile
  import dbConfig.db
  import profile.api._
  import guardedCall.implicits._

  private case class CartItemRow(
    productId: Long,
    amount:    Int,
    userId:    UUID,
    id:        Long = -1
  )

  private class CartItems(tag: Tag) extends Table[CartItemRow](tag, "cart_items") {
    def productId = column[Long]("product_id")
    def amount = column[Int]("amount")
    def userId = column[UUID]("user_id")
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def * = (productId, amount, userId, id).mapTo[CartItemRow]
  }

  private val baseQuery = TableQuery[CartItems]

  private val byUserIdQuery = Compiled { userId: Rep[UUID] =>
    baseQuery.filter(_.userId === userId)
  }

  def find(user: UUID)(implicit ec: ExecutionContext): Future[Seq[CartItem]] = db.run {
    byUserIdQuery(user).result.map(_.map(row => CartItem(ProductId(row.productId), row.amount)))
  }.guarded

  def delete(user: UUID)(implicit ec: ExecutionContext): Future[Unit] = db.run {
    DBIO.seq(byUserIdQuery(user).delete)
  }.guarded

  def insert(user: UUID, item: CartItem)(implicit ec: ExecutionContext): Future[Unit] = db.run {
    DBIO.seq(baseQuery += CartItemRow(item.product.value, item.amount, user))
  }.guarded

}
