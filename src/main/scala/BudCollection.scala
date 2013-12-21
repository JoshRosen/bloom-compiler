


abstract class BudCollection[T]  {

  def <+(other: BudCollection[T]): DeferredMerge[T] =
    new DeferredMerge[T](this, other)

  def <=(other: BudCollection[T]): InstantMerge[T] =
    new InstantMerge[T](this, other)

  def <=(value: T): InstantMergeSingle[T] =
    new InstantMergeSingle[T](this, value)

  def join[U](other: BudCollection[U])(condition: Any): BudCollection[(T, U)] =
    new JoinCollection[T, U](this, other, condition)

  // TODO: same result-type

  //  def <+-(other: BudCollection[T])


  // schema
  // keys
  // cols
  // name
}


class JoinCollection[T, U](left: BudCollection[T], right: BudCollection[U], condition: Any)
  extends BudCollection[(T, U)] {

}



sealed trait Rule {}

case class DeferredMerge[T](left: BudCollection[T], right: BudCollection[T]) extends Rule
case class InstantMerge[T](left: BudCollection[T], right: BudCollection[T]) extends Rule
case class InstantMergeSingle[T](left: BudCollection[T], right: T) extends Rule
case class Join[T, U](left: BudCollection[T], right: BudCollection[U], condition: Any) extends Rule