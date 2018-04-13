package pl.edu.agh.msc.perftests

import io.gatling.core.Predef.exec

object CommonActions {

  def productDetails(id: String) = exec(get("Products details", s"/products/$id"))

}
