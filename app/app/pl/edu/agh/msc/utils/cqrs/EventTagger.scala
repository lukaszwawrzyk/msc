package pl.edu.agh.msc.utils.cqrs

import akka.persistence.journal.{ EventAdapter, EventSeq, Tagged }

class EventTagger(companion: EntityCompanion) extends EventAdapter {
  import companion.eventClass

  override def manifest(event: Any): String = ""
  override def toJournal(event: Any): Any = event match {
    case e: companion.Event =>
      Tagged(e, Set(companion.tag))
    case _ =>
      event
  }
  override def fromJournal(event: Any, manifest: String): EventSeq = {
    EventSeq.single(event)
  }
}