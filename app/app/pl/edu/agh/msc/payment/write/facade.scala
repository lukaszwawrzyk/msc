package pl.edu.agh.msc.payment.write

import akka.actor.{ ActorSystem, Props }
import javax.inject.{ Inject, Singleton }
import pl.edu.agh.msc.payment.PaymentId
import pl.edu.agh.msc.utils.NotificationService
import pl.edu.agh.msc.utils.cqrs.EntitiesFacade

@Singleton class PaymentEntitiesFacade @Inject() (
  val actorSystem:     ActorSystem,
  notificationService: NotificationService
) extends EntitiesFacade(PaymentEntity) {

  override protected def props: Props = {
    Props(new PaymentEntity(notificationService))
  }

}
