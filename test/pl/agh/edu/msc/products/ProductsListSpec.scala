package pl.agh.edu.msc.products

import pl.agh.edu.msc.common.IntegrationTest
import cats.syntax.option._
import pl.agh.edu.msc.review.Rating

class ProductsListSpec extends IntegrationTest {

  private val productsService = inject[ProductService]
  private val productsRepository = inject[ProductRepository]

  it should "paginate filtered results" in {
    // GIVEN
    insertProduct(name = "a", rating = 4.1)
    insertProduct(name = "b", rating = 4.0)
    insertProduct(name = "c", rating = 4.0)
    insertProduct(name = "d", rating = 2.0)
    insertProduct(name = "e", rating = 5.0)
    insertProduct(name = "f", rating = 4.0)
    insertProduct(name = "g", rating = 4.0)
    insertProduct(name = "h", rating = 4.0)

    def page(page: Int) = {
      productsService.list(
        Filtering(minRating = Rating(4).some),
        Pagination(size = 2, page = page),
        Sorting(byNameAsc = true)
      ).await()
    }

    // WHEN
    val page1 = page(1)
    val page2 = page(2)
    val page3 = page(3)
    val page4 = page(4)

    // THEN
    def checkPage(paginated: Paginated[ProductListItem], page: Int, expectedProducts: String*) = {
      paginated.totalPages shouldBe 4
      paginated.pagination shouldBe Pagination(size = 2, page)
      paginated.data.map(_.name) shouldBe expectedProducts
    }

    checkPage(page1, page = 1, expectedProducts = "a", "b")
    checkPage(page2, page = 2, expectedProducts = "c", "e")
    checkPage(page3, page = 3, expectedProducts = "f", "g")
    checkPage(page4, page = 4, expectedProducts = "h")
  }

  private def insertProduct(name: String, rating: Double) = {
    val product = ProductRepoView(
      name,
      cachedPrice = Money(12),
      photo = None,
      cachedAverageRating = Some(Rating(rating)),
      description = "something"
    )
    productsRepository.insert(product).await()
  }
}
