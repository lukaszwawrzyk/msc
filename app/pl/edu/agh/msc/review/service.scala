package pl.edu.agh.msc.review

import javax.inject.{ Inject, Singleton }

import pl.edu.agh.msc.products.ProductId

import scala.concurrent.{ ExecutionContext, Future }

@Singleton class ReviewService @Inject() (reviewRepository: ReviewRepository) {

  def find(id: ProductId)(implicit ec: ExecutionContext): Future[Seq[Review]] = {
    reviewRepository.find(id)
  }

  def latest(limit: Int)(implicit ec: ExecutionContext): Future[Seq[Review]] = {
    reviewRepository.latest(limit)
  }

  def averageRating(id: ProductId)(implicit ec: ExecutionContext): Future[Option[Rating]] = {
    reviewRepository.averageRating(id)
  }

  def add(id: ProductId, review: Review)(implicit ec: ExecutionContext): Future[Unit] = {
    reviewRepository.insert(id, review)
  }

}
