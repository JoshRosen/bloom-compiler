package edu.berkeley.cs.boom.bloomscala.collections

import scala.collection.mutable
import edu.berkeley.cs.boom.bloomscala.Bud
import com.typesafe.scalalogging.slf4j.Logging

class Table[T](implicit bud: Bud) extends BudCollection[T] with Logging {
  private val toDelete = new mutable.HashSet[T]

  override def doPendingDelete(item: T) {
    toDelete += item
  }

  override def tick() {
    storage --= toDelete
    toDelete.clear()
    super.tick()
  }
}