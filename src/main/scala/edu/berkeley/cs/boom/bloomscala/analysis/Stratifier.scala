package edu.berkeley.cs.boom.bloomscala.analysis

import edu.berkeley.cs.boom.bloomscala.parser.AST._
import org.kiama.attribution.Attribution._
import org.kiama.util.Messaging

class Stratifier(messaging: Messaging, namer: Namer, depAnalyzer: DepAnalayzer) {

  import depAnalyzer._
  import namer._

  lazy val isTemporallyStratifiable: Program => Boolean =
    attr { program =>
      !program.statements.exists { stmt =>
        participatesInNonTemporalCycle(stmt) && statementDependencies(stmt).exists(_.isNegated)
      }
    }

  lazy val stratifiedRules = null
}
