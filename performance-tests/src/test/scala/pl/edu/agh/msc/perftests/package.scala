package pl.edu.agh.msc

import io.gatling.core.Predef._
import io.gatling.http.Predef._

package object perftests {

  def get(name: String, uri: String) = http(name).get(uri)
  def post(name: String, uri: String) = http(name).post(uri)

}
