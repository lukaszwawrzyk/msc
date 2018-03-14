package pl.edu.agh.msc.pricing

case class Money(value: BigDecimal) extends AnyVal {
  def *(multiplier: Int): Money = Money(value * multiplier)
  def +(addend: Money): Money = Money(value + addend.value)
}
