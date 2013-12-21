package edu.berkeley.cs.boom.bloomscala.collections

import scala.collection.mutable
import edu.berkeley.cs.boom.bloomscala.{Bud, InstantMergeSingle, InstantMerge, DeferredMerge}


abstract class BudCollection[T](implicit bud: Bud) {

  def <+(other: BudCollection[T]): DeferredMerge[T] =
    new DeferredMerge[T](this, other)

  def <=(other: BudCollection[T]): InstantMerge[T] =
    new InstantMerge[T](this, other)

  def <=(value: T): InstantMergeSingle[T] =
    new InstantMergeSingle[T](this, value)

  def join[U, K](other: BudCollection[U], leftKey: T => K, rightKey: U => K): BudCollection[(T, U)] = {
    new JoinCollection[T, U, K](this, other)(leftKey, rightKey)
  }

  def map[R](f: T => R): BudCollection[R] = new MappedBudCollection[R, T](this, f)

  def size: Int = 0  // TODO

  def doInsert(item: T) {}  // TODO

  def foreach(f: T => Unit) {}
  //  def <+-(other: BudCollection[T])

  // schema
  // keys
  // cols
  // name
}

class MappedBudCollection[U, T](prev: BudCollection[T], mapF: T => U)(implicit bud: Bud) extends BudCollection[U] {
  override def foreach(f: U => Unit) {
    prev.foreach(x => f(mapF(x)))
  }
}


class JoinCollection[T, U, K](left: BudCollection[T], right: BudCollection[U])(leftKey: T => K, rightKey: U => K)(implicit bud: Bud)
  extends BudCollection[(T, U)] {

  // TODO: This is really naive and inefficient:
  override def foreach(f: ((T, U)) => Unit) {
    val table = new mutable.HashMap[K, T]
    left.foreach{x => table += ((leftKey(x), x)) }
    right.foreach{r => table.get(rightKey(r)).foreach(l => f(l, r))}
  }
}


