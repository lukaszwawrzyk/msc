package pl.edu.agh.msc

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.ScalaModule
import pl.edu.agh.msc.utils._

class ShopModule extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    bind[Time].toInstance(new RealTime)
    bind[NotificationService].to[WSNotificationService].asEagerSingleton
  }

}

class InitModule extends AbstractModule with ScalaModule {

  override def configure(): Unit = {
    bind[InitDb].asEagerSingleton
  }

}
