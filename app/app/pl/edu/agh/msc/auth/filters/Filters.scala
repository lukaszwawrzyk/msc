package pl.edu.agh.msc.auth.filters

import akka.stream.Materializer
import javax.inject.Inject
import play.api.Logger
import play.api.http.HttpFilters
import play.api.mvc.{ EssentialFilter, _ }
import play.filters.headers.SecurityHeadersFilter

import scala.concurrent.{ ExecutionContext, Future }

class LoggingFilter @Inject() (implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    val startTime = System.currentTimeMillis
    nextFilter(requestHeader).map { result =>
      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime
      if (!requestHeader.uri.startsWith("/assets")) {
        Logger.info(s"${requestHeader.method} ${requestHeader.uri} took ${requestTime}ms and returned ${result.header.status}")
      }
      result.withHeaders("Request-Time" -> requestTime.toString)
    }
  }

}

class Filters @Inject() (securityHeadersFilter: SecurityHeadersFilter, loggingFilter: LoggingFilter) extends HttpFilters {
  override def filters: Seq[EssentialFilter] = Seq(/*loggingFilter, */securityHeadersFilter)
}
