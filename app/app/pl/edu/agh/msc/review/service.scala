package pl.edu.agh.msc.review

import javax.inject.{ Inject, Singleton }

import pl.edu.agh.msc.products.ProductId


@Singleton class ReviewService @Inject() (reviewRepository: ReviewRepository) {

  def find(id: ProductId): Seq[Review] = {
    reviewRepository.find(id)
  }

  def latest(limit: Int): Seq[(Review, ProductId)] = {
    reviewRepository.latest(limit)
  }

  def averageRating(id: ProductId): Option[Rating] = {
    reviewRepository.averageRating(id)
  }

  def add(id: ProductId, review: Review): Unit = {
    reviewRepository.insert(id, review)
  }

}
