package pl.edu.agh.msc.auth.jobs

import java.time.{ LocalDateTime, ZoneOffset }
import java.util.Date

import akka.actor.{ ActorRef, ActorSystem }
import com.google.inject.Inject
import com.google.inject.name.Named
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension

class Scheduler @Inject() (
  system:                                        ActorSystem,
  @Named("auth-token-cleaner") authTokenCleaner: ActorRef
) {
  QuartzSchedulerExtension(system).schedule(
    "AuthTokenCleaner",
    authTokenCleaner,
    AuthTokenCleaner.Clean,
    startDate = Some(Date.from(LocalDateTime.now().plusMinutes(1).toInstant(ZoneOffset.UTC)))
  )
}
