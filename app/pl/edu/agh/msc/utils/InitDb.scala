package pl.edu.agh.msc.utils

import java.time.{ LocalDateTime, ZoneId }
import java.util.concurrent.TimeUnit
import java.util.{ Date, Locale, Random }
import javax.inject.Inject

import com.github.javafaker.Faker
import controllers.AssetsFinder
import pl.edu.agh.msc.pricing.{ Money, PriceRepository }
import pl.edu.agh.msc.products.{ ProductId, ProductRepoView, ProductRepository, ProductService }
import pl.edu.agh.msc.review.{ Rating, Review, ReviewRepository }

import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.implicitConversions

class InitDb @Inject() (
  productService:     ProductService,
  productsRepository: ProductRepository,
  pricesRepository:   PriceRepository,
  reviewRepository:   ReviewRepository,
  assetsFinder:       AssetsFinder
)(implicit ec: ExecutionContext) {

  Locale.setDefault(Locale.ENGLISH)

  private val faker = new Faker(new Random())

  println("Initializing data")
  Future.traverse(1 to 1000)(_ => createAndSaveProduct()).foreach(_ => println("Initialized"))

  private def createAndSaveProduct(): Future[ProductId] = {
    val category = faker.resolve("commerce.department")
    val name = faker.commerce.productName()
    val price = Money(BigDecimal(faker.commerce.price(1, 2000)))
    val photo = {
      val i = faker.random.nextInt(4) + 1
      assetsFinder.path(s"images/p_$i.jpg")
    }
    for {
      id <- productsRepository.insert(ProductRepoView(
        name,
        Money(0),
        photo               = Some(photo),
        cachedAverageRating = None,
        description         = ""
      ))
      _ <- pricesRepository.save(id, price)
      _ <- Future.traverse(0 to faker.random.nextInt(15))(_ => reviewRepository.insert(id, createReview()))
      _ <- productService.findDetailed(id) // triggers cache update
    } yield id
  }

  private def createReview(): Review = {
    val author = faker.name.fullName()
    val content = faker.lorem.sentences(faker.random().nextInt(10) + 1).asScala.mkString(" ")
    val rating = sample(Map(1 -> 0.08, 2 -> 0.02, 3 -> 0.2, 4 -> 0.25, 5 -> 0.45))
    val date = faker.date.past(1000, TimeUnit.DAYS)

    Review(author, content, Rating(rating.toDouble), date)
  }

  implicit def dateToLocalDateTime(date: Date): LocalDateTime = {
    LocalDateTime.ofInstant(date.toInstant, ZoneId.systemDefault)
  }

  private def sample[A](dist: Map[A, Double]): A = {
    val p = scala.util.Random.nextDouble
    val it = dist.iterator
    var accum = 0.0
    while (it.hasNext) {
      val (item, itemProb) = it.next
      accum += itemProb
      if (accum >= p)
        return item // return so that we don't have to search through the whole distribution
    }
    sys.error(f"this should never happen") // needed so it will compile
  }

}