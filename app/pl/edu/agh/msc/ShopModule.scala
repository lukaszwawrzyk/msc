package pl.edu.agh.msc

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import pl.edu.agh.msc.payment.{ NotificationService, WSNotificationService }
import pl.edu.agh.msc.utils.{ InitDb, RealTime, Time }

class ShopModule extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    bind[Time].toInstance(new RealTime)
    bind[NotificationService].to[WSNotificationService].asEagerSingleton
    bind[InitDb].asEagerSingleton
  }

}
