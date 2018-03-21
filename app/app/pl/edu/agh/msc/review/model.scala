package pl.edu.agh.msc.review

import java.time.LocalDateTime

case class Rating(value: Double) { require(value <= 5.0 && value >= 1) }

case class Review(
  author:  String,
  content: String,
  rating:  Rating,
  date:    LocalDateTime
)

