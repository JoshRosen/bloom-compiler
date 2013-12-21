package edu.berkeley.cs.boom.bloomscala

import com.typesafe.scalalogging.slf4j.Logging
import edu.berkeley.cs.boom.bloomscala.collections.BudCollection

class Stratum(rules: Iterable[Rule], tables: Traversable[BudCollection[_]]) extends Logging {
  def fixpoint() {
    val (oneShotRules, multiShotRules) = rules.partition(_.isInstanceOf[OneShotRule])
    oneShotRules.foreach(_.evaluate())
    do {
      logger.debug("Computing fixpoint")
      multiShotRules.foreach(_.evaluate())
    } while (tables.map(_.mergeDeltas()).fold(false)(_ || _))
    logger.debug("Finished computing fixpoint")
  }
}
