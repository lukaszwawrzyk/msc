package pl.edu.agh.msc.orders.write

import pl.edu.agh.msc.utils.cqrs.EventTagger

class OrderEventTagger extends EventTagger(OrderEntity)