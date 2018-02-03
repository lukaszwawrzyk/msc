package example

import org.scalatest._

class Test extends FlatSpec with Matchers {
  it should "add numbers" in {
    2 + 2 shouldBe 4
  }
}
