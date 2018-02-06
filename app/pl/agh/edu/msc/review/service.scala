package pl.agh.edu.msc.review

import javax.inject.{ Inject, Singleton }

import pl.agh.edu.msc.products.ProductId

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class ReviewService @Inject() (reviewRepository: ReviewRepository) {

  def reviews(id: ProductId)(implicit ec: ExecutionContext): Future[Seq[Review]] = {
    reviewRepository.find(id)
  }

  def averageRating(id: ProductId)(implicit ec: ExecutionContext): Future[Option[Rating]] = {
    reviewRepository.averageRating(id)
  }

}
