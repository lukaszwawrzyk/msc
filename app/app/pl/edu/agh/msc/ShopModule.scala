package pl.edu.agh.msc

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import pl.edu.agh.msc.payment.{ NotificationService, WSNotificationService }
import pl.edu.agh.msc.utils._

class ShopModule extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    bind[Time].to[RealTime].asEagerSingleton
    bind[NotificationService].to[WSNotificationService].asEagerSingleton
    bind[SecuredController].to[AsyncSecuredController]
    bind[GuardedCall].asEagerSingleton
  }

}

class InitModule extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    bind[InitDb].asEagerSingleton
  }

}
