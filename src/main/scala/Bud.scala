// https://github.com/bloom-lang/bud/blob/101ad1985ebccbd5f814365a89c6b5af293b5dbe/lib/bud.rb

class Bud(tables: Iterable[Table[_]], stratifiedRules: Iterable[Iterable[Rule]]) {

  var budtime = 0

  val strata = stratifiedRules.map(new Stratum(_, tables))

  def tick() {
    // Receive inbound
    strata.foreach(_.fixpoint())
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

class Scratch {}

class Channel {}

class Periodic {}

