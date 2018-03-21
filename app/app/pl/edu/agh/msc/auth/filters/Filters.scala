package pl.edu.agh.msc.auth.filters

import javax.inject.Inject

import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import play.filters.headers.SecurityHeadersFilter

class Filters @Inject() (securityHeadersFilter: SecurityHeadersFilter) extends HttpFilters {
  override def filters: Seq[EssentialFilter] = Seq(securityHeadersFilter)
}
