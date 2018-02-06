package pl.agh.edu.msc.products

import java.net.URL

import pl.agh.edu.msc.review.{ Rating, Review }

case class ProductId(value: Long) extends AnyVal

case class ProductListItem(
  name:          String,
  price:         Money,
  photo:         Option[URL],
  averageRating: Option[Rating],
  id:            ProductId
)

case class ProductDetails(
  name:          String,
  price:         Money,
  photo:         Option[URL],
  description:   String,
  averageRating: Option[Rating],
  reviews:       Seq[Review],
  availability:  Option[Availability],
  id:            ProductId
)

case class Money(value: BigDecimal) extends AnyVal

case class Availability(value: BigDecimal) extends AnyVal