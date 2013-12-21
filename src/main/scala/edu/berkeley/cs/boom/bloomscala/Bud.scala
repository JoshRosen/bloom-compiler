package edu.berkeley.cs.boom.bloomscala

import edu.berkeley.cs.boom.bloomscala.collections.{Scratch, BudCollection}
import scala.collection.mutable
import com.typesafe.scalalogging.slf4j.Logging

class Bud extends Logging {

  private val tables: mutable.Set[BudCollection[_]] = new mutable.HashSet[BudCollection[_]]
  private val stratifiedRules: mutable.ArrayBuffer[Iterable[Rule]] = new mutable.ArrayBuffer[Iterable[Rule]]

  private[bloomscala] def addTable(table: BudCollection[_]) {
    tables += table
  }

  def addStrata(rules: Iterable[Rule]) {
    stratifiedRules += rules
  }

  def strata = stratifiedRules.map(new Stratum(_, tables))

  var budtime = 0

  def tick() {
    // Receive inbound
    tables.foreach(_.tick())
    strata.zipWithIndex.foreach{ case (stratum, index) =>
      logger.debug(s"Computing stratum $index")
      stratum.fixpoint()
    }
    // Flush channels in order
    // Reset periodics
  }
  // Strata
  // Budtime
  // Connections
  // Tables

  // ip
  // port
  // connections
  // inbound

  // periodics
  // vars
  // tempvars

  /** Methods for controlling execution */

  //def run()

  //def tick()

 // def receive_inbound

  //def stratum_fixpoint

 // def reset_periodics


  // Methods
  // Join
  // Natural join
  //
}