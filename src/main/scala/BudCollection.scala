import com.typesafe.scalalogging.slf4j.Logging
import scala.collection.mutable


abstract class BudCollection[T] {

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

class MappedBudCollection[U, T](prev: BudCollection[T], mapF: T => U) extends BudCollection[U] {
  override def foreach(f: U => Unit) {
    prev.foreach(x => f(mapF(x)))
  }
}


class JoinCollection[T, U, K](left: BudCollection[T], right: BudCollection[U])(leftKey: T => K, rightKey: U => K)
  extends BudCollection[(T, U)] {

  // TODO: This is really naive and inefficient:
  override def foreach(f: ((T, U)) => Unit) {
    val table = new mutable.HashMap[K, T]
    left.foreach{x => table += ((leftKey(x), x)) }
    right.foreach{r => table.get(rightKey(r)).foreach(l => f(l, r))}
  }
}

sealed trait Rule extends Logging {
  def evaluate() = {}
}

trait OneShotRule extends Rule

case class DeferredMerge[T](left: BudCollection[T], right: BudCollection[T]) extends Rule

case class InstantMerge[T](left: BudCollection[T], right: BudCollection[T]) extends Rule {
  override def evaluate() {
    logger.debug("Evaluating")
    right.foreach(left.doInsert)
  }
}

case class InstantMergeSingle[T](left: BudCollection[T], right: T) extends OneShotRule {
  override def evaluate() {
    logger.debug("Evaluating")
    left.doInsert(right)
  }
}
case class Join[T, U, K](left: BudCollection[T], right: BudCollection[U]) extends Rule