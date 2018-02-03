package pl.agh.edu.msc.products

import java.net.URL

case class ProductId(value: Long) extends AnyVal

case class ProductListView(
  name:          String,
  price:         Money,
  photo:         Option[URL],
  averageRating: Option[Rating],
  id:            ProductId
)

case class ProductFullView(
  name:          String,
  price:         Money,
  photo:         Option[URL],
  description:   String,
  averageRating: Option[Rating],
  reviews:       Seq[ReviewView],
  availability:  Option[Availability],
  id:            ProductId
)

case class ReviewView(
  author:  String,
  content: String,
  rating:  Rating
)

case class Rating(value: Double) { require(value <= 5.0 && value >= 1) }

case class Money(value: BigDecimal) extends AnyVal

case class Availability(value: BigDecimal) extends AnyVal