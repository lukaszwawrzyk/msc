package pl.edu.agh.msc.cart

import java.util.UUID

import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.products.ProductId
import pl.edu.agh.msc.utils.{ SlickTypeMappings, _ }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton class CartRepository @Inject() (dbConfigProvider: DatabaseConfigProvider) extends SlickTypeMappings {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  protected val profile = dbConfig.profile
  import dbConfig.db
  import profile.api._

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

  def find(user: UUID): Seq[CartItem] = {
    db.run(byUserIdQuery(user).result).await().map(row => CartItem(ProductId(row.productId), row.amount))
  }

  def delete(user: UUID): Unit = db.run {
    byUserIdQuery(user).delete
  }.await()

  def insert(user: UUID, item: CartItem): Unit = db.run {
    baseQuery += CartItemRow(item.product.value, item.amount, user)
  }.await()

}
