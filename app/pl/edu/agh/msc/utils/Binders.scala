package pl.edu.agh.msc.utils

import java.util.UUID

import pl.edu.agh.msc.orders.OrderId
import pl.edu.agh.msc.products.ProductId
import play.api.mvc.PathBindable
import play.api.mvc.PathBindable.Parsing

object Binders {

  implicit object UUIDPathBindable extends Parsing[UUID](
    UUID.fromString, _.toString, (key: String, e: Exception) => "Cannot parse parameter %s as UUID: %s".format(key, e.getMessage)
  )

  implicit val productIdBindable: PathBindable[ProductId] =
    implicitly[PathBindable[Long]].transform(ProductId(_), _.value)

  implicit val orderIdBindable: PathBindable[OrderId] =
    implicitly[PathBindable[UUID]].transform(OrderId(_), _.value)

}
