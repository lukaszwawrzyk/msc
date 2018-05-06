package pl.edu.agh.msc.orders.write

import akka.persistence.journal.{ EventAdapter, EventSeq, Tagged }

class OrderEventTagger extends EventAdapter {
  override def manifest(event: Any): String = ""

  override def toJournal(event: Any): Any = event match {
    case e: OrderEntity.Event =>
      Tagged(e, Set("orders"))
    case _ =>
      event
  }

  override def fromJournal(event: Any, manifest: String): EventSeq = {
    EventSeq.single(event)
  }
}