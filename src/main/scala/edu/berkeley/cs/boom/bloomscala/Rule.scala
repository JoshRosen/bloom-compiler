package edu.berkeley.cs.boom.bloomscala

import com.typesafe.scalalogging.slf4j.Logging
import edu.berkeley.cs.boom.bloomscala.collections.BudCollection

sealed trait Rule extends Logging {
  private[bloomscala] def evaluate() { throw new AbstractMethodError }
}

trait OneShotRule extends Rule

case class DeferredMerge[T](left: BudCollection[T], right: BudCollection[T]) extends Rule

case class DeferredMergeSingle[T](left: BudCollection[T], right: T) extends OneShotRule {
  override def evaluate() {
    logger.debug("Evaluating")
    left.doPendingInsert(right)
  }
}

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