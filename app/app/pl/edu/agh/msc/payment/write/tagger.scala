package pl.edu.agh.msc.payment.write

import pl.edu.agh.msc.utils.cqrs.EventTagger

class PaymentEventTagger extends EventTagger(PaymentEntity)
