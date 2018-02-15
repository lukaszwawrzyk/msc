package pl.edu.agh.msc.products

import java.net.URL

import pl.edu.agh.msc.availability.Availability
import pl.edu.agh.msc.pricing.Money
import pl.edu.agh.msc.review.{ Rating, Review }

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