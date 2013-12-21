import scala.collection.mutable


abstract class BudCollection[T] {

  private val storage = new mutable.HashMap

  def <+(other: BudCollection[T]): DeferredMerge[T] =
    new DeferredMerge[T](this, other)

  def <=(other: BudCollection[T]): InstantMerge[T] =
    new InstantMerge[T](this, other)

  def <=(value: T): InstantMergeSingle[T] =
    new InstantMergeSingle[T](this, value)

  def join[U, K](other: BudCollection[U], leftKey: T => K, rightKey: U => K): BudCollection[(T, U)] = {
    new JoinCollection[T, U, K](this, other)(leftKey, rightKey)
  }

  def map[R](f: T => R): BudCollection[R] = this.asInstanceOf[BudCollection[R]]

  def size: Int = 0
  //  def <+-(other: BudCollection[T])

  // schema
  // keys
  // cols
  // name
}


class JoinCollection[T, U, K](left: BudCollection[T], right: BudCollection[U])(leftKey: T => K, rightKey: U => K)
  extends BudCollection[(T, U)] {

}

sealed trait Rule {}

case class DeferredMerge[T](left: BudCollection[T], right: BudCollection[T]) extends Rule
case class InstantMerge[T](left: BudCollection[T], right: BudCollection[T]) extends Rule
case class InstantMergeSingle[T](left: BudCollection[T], right: T) extends Rule
case class Join[T, U, K](left: BudCollection[T], right: BudCollection[U]) extends Rule