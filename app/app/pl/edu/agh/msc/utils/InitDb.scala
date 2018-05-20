package pl.edu.agh.msc.utils

import java.time.{ LocalDateTime, ZoneId }
import java.util.concurrent.TimeUnit
import java.util.{ Date, Locale, Random }

import javax.inject.Inject
import com.github.javafaker.Faker
import controllers.AssetsFinder
import pl.edu.agh.msc.availability.{ Availability, AvailabilityRepository }
import pl.edu.agh.msc.orders.read.OrdersEventMapper
import pl.edu.agh.msc.payment.read.PaymentEventMapper
import pl.edu.agh.msc.pricing.{ Money, PriceRepository }
import pl.edu.agh.msc.products._
import pl.edu.agh.msc.review.{ Rating, Review, ReviewRepository }

import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.implicitConversions

class InitDb @Inject() (
  productService:         ProductService,
  productsRepository:     ProductRepository,
  pricesRepository:       PriceRepository,
  reviewRepository:       ReviewRepository,
  availabilityRepository: AvailabilityRepository,
  assetsFinder:           AssetsFinder,
  ordersEventMapper:      OrdersEventMapper,
  paymentsEventMapper:    PaymentEventMapper
)(implicit ec: ExecutionContext) {

  Locale.setDefault(Locale.ENGLISH)

  private val faker = new Faker(new Random())

  println("Initializing data")
  def run(left: Int): Future[ProductId] = {
    if (left == 0) {
      createAndSaveProduct()
    } else {
      createAndSaveProduct().flatMap(_ => run(left - 1))
    }
  }

  productService.listNonCached(Filtering(), Pagination(1, 1)).flatMap { res =>
    if (res.data.isEmpty) run(500).map(_ => println("Initialized")) else Future.successful(println("Already initialized"))
  }.map { _ =>
    println("starting mappers")
    ordersEventMapper.run()
    paymentsEventMapper.run()
    println("started mappers")
  }

  private def createAndSaveProduct(): Future[ProductId] = {
    val category = faker.resolve("commerce.department")
    val name = faker.commerce.productName()
    val price = Money(BigDecimal(faker.commerce.price(2, 2000)))
    val photo = {
      val i = faker.random.nextInt(4) + 1
      assetsFinder.path(s"images/p_$i.jpg")
    }
    val description = {
      val paragraphs = List.fill(faker.random.nextInt(1) + 3)(faker.lorem.paragraph(faker.random.nextInt(8) + 2))
        .map(p => "<p>" + p + "</p>")
      val features = List.fill(faker.random.nextInt(7) + 3)(faker.lorem.sentence(2, 5))
        .map(f => "<li>" + f + "</li>").mkString("<ul>", "", "</ul>")
      val allParagraphs = paragraphs match {
        case first :: rest => first :: features :: rest
        case Nil           => features :: Nil
      }
      allParagraphs.mkString
    }
    val stock = Availability(faker.random.nextLong(100))
    for {
      id <- productsRepository.insert(ProductRepoView(
        name,
        Money(0),
        photo               = Some(photo),
        cachedAverageRating = None,
        description         = description
      ))
      _ <- pricesRepository.save(id, price)
      _ <- availabilityRepository.save(id, stock)
      _ <- Future.traverse(0 to faker.random.nextInt(15))(_ => reviewRepository.insert(id, createReview()))
      _ <- productService.updateCache(id) // triggers cache update
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