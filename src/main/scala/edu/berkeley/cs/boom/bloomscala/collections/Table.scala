package edu.berkeley.cs.boom.bloomscala.collections

import scala.collection.mutable
import edu.berkeley.cs.boom.bloomscala.Bud

class Table[T](implicit bud: Bud) extends BudCollection[T] {
  private val storage = new mutable.HashSet[T]

  override def size: Int = storage.size

  override def doInsert(item: T) {
    storage += item
  }

  override def foreach(f: T => Unit) { storage.foreach(f) }

}