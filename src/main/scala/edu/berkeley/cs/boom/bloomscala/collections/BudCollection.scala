package edu.berkeley.cs.boom.bloomscala.collections

import scala.collection.mutable
import edu.berkeley.cs.boom.bloomscala._
import edu.berkeley.cs.boom.bloomscala.InstantMerge
import edu.berkeley.cs.boom.bloomscala.InstantMergeSingle
import edu.berkeley.cs.boom.bloomscala.DeferredMerge
import com.typesafe.scalalogging.slf4j.Logging


abstract class BudCollection[T](implicit bud: Bud) extends Logging {

  bud.addTable(this)

  /** "Normal" tuples */
  protected val storage = new mutable.HashSet[T]
  /** Delta for the RHS of rules during semi-naive evaluation */
  protected val delta: mutable.Set[T] = new mutable.HashSet[T]
  /** The LHS tuples currently being produced during semi-naive evaluation */
  protected val newDelta: mutable.Set[T] = new mutable.HashSet[T]
  /** Tuples deferred until the next tick */
  private val pending = new mutable.HashSet[T]

  def <+(other: BudCollection[T]): Rule =
    new DeferredMerge[T](this, other)

  def <+(value: T): Rule =
    new DeferredMergeSingle[T](this, value)

  def <=(other: BudCollection[T]): Rule =
    new InstantMerge[T](this, other)

  def <=(value: T): Rule =
    new InstantMergeSingle[T](this, value)

  def join[U, K](other: BudCollection[U], leftKey: T => K, rightKey: U => K): BudCollection[(T, U)] = {
    new JoinCollection[T, U, K](this, other)(leftKey, rightKey)
  }

  def map[R](f: T => R): BudCollection[R] = new MappedBudCollection[R, T](this, f)

  def size: Int = storage.union(delta).size

  private[bloomscala] def doInsert(item: T) {
    delta += item
  }

  private[bloomscala] def doPendingInsert(item: T) {
    pending += item
  }

  private[bloomscala] def doPendingDelete(item: T) {}  // TODO

  def foreach(f: T => Unit) { storage.union(delta).foreach(f) }

  private[bloomscala] def mergeDeltas(): Boolean = {
    // TODO: should this use newDelta?
    // Returns 'true' if we produced a delta during this round of semi-naive evaluation.
    val changed = delta.map { storage.add }.fold(false)(_ || _)
    delta.clear()
    changed
  }

  private[bloomscala] def tick() {
    if (!pending.isEmpty) {
      logger.debug(s"Merging pending inserts: $pending")
    }
    storage ++= pending
    pending.clear()
  }
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


