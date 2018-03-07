package pl.edu.agh.msc.utils

import java.util.UUID

import pl.edu.agh.msc.products.ProductId
import play.api.mvc.PathBindable

object Binders {

  implicit object UUIDPathBindable extends PathBindable[UUID] {
    def bind(key: String, value: String): Either[String, UUID] = try {
      Right(UUID.fromString(value))
    } catch {
      case _: Exception => Left("Cannot parse parameter '" + key + "' with value '" + value + "' as UUID")
    }

    def unbind(key: String, value: UUID): String = value.toString
  }

  implicit object ProductIdBindable extends PathBindable[ProductId] {
    private val longBindable = implicitly[PathBindable[Long]]
    override def bind(key: String, value: String): Either[String, ProductId] = longBindable.bind(key, value).map(ProductId)
    override def unbind(key: String, value: ProductId): String = longBindable.unbind(key, value.value)
  }

}
