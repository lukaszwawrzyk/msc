package models

import play.api.libs.json._

case class Person(name: String, age: Int, id: Long = -1)

object Person {  
  implicit val personFormat = Json.format[Person]
}
