package pl.edu.agh.msc.perftests

import java.util.concurrent.ThreadLocalRandom

import com.github.javafaker.Faker

class Random(lastProductId: Int) {
  private def javaRandom = ThreadLocalRandom.current()
  private def scalaRandom = new scala.util.Random(javaRandom)

  def fake = new Faker(javaRandom)
  def range(from: Int, to: Int): Int = scalaRandom.nextInt(to - from) + from
  def productId(): Int = range(1, lastProductId)
  def gaussian(): Double = scalaRandom.nextGaussian()
}
