package edu.berkeley.cs.boom.bloomscala

import edu.berkeley.cs.boom.bloomscala.collections.{Scratch, BudCollection}
import scala.collection.mutable

class Bud {

  private val tables: mutable.Set[BudCollection[_]] = new mutable.HashSet[BudCollection[_]]
  private val stratifiedRules: mutable.ArrayBuffer[Iterable[Rule]] = new mutable.ArrayBuffer[Iterable[Rule]]

  def addStrata(rules: Iterable[Rule]) {
    stratifiedRules += rules
  }

  def strata = stratifiedRules.map(new Stratum(_, tables))

  var budtime = 0

  def tick() {
    // Receive inbound
    strata.foreach(_.fixpoint())
    // Flush channels in order
    // Reset periodics
    tables.foreach {
      case scratch: Scratch[_] =>
        scratch.clear()
      case _ =>
    }
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