package edu.berkeley.cs.boom.bloomscala


trait ExemplaryAggregate[T] {
  def aggregate(state: T, obj: T): T
}

class Min[T : Ordering] extends ExemplaryAggregate[T] {
  def aggregate(state: T, obj: T) = {
    implicitly[Ordering[T]].min(state, obj)
  }
}
