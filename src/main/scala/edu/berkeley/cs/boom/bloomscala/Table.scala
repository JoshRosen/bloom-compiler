package edu.berkeley.cs.boom.bloomscala

import scala.collection.mutable

class Table[T] extends BudCollection[T] {
  private val storage = new mutable.HashSet[T]

  override def size: Int = storage.size

  override def doInsert(item: T) {
    storage += item
  }

  override def foreach(f: T => Unit) { storage.foreach(f) }

}