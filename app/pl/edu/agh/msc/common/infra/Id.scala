package pl.edu.agh.msc.common.infra

import slick.lifted.MappedTo

case class Id[A](value: Long) extends AnyVal with MappedTo[Long]
