package edu.berkeley.cs.boom.bloomscala.analysis

import edu.berkeley.cs.boom.bloomscala.parser.AST._
import org.kiama.attribution.Attribution._
import org.kiama.util.Messaging

class Stratifier(messaging: Messaging, depAnalyzer: DepAnalayzer) {

  import depAnalyzer._

  lazy val isTemporallyStratifiable: Program => Boolean =
    attr { program =>
      program.statements.forall(!participatesInCycle(_))
    }

  lazy val stratifiedRules = null
}
