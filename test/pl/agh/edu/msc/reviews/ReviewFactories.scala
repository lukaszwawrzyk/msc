package pl.agh.edu.msc.reviews

import java.time.LocalDateTime

import pl.agh.edu.msc.review.{ Rating, Review }

trait ReviewFactories {

  protected def createReview(
    author: String = "John",
    content: String = "nice",
    rating: Int = 4,
    date: LocalDateTime = LocalDateTime.of(2018, 2, 7, 12, 30)
  ) = {
    Review(author, content, Rating(rating), date)
  }


}
