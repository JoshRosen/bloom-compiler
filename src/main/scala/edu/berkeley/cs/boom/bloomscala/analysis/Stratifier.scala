package edu.berkeley.cs.boom.bloomscala.analysis

import edu.berkeley.cs.boom.bloomscala.parser.AST._
import org.kiama.attribution.Attribution._
import org.kiama.util.Messaging

class Stratifier(messaging: Messaging, namer: Namer, depAnalyzer: DepAnalayzer) {

  import depAnalyzer._
  import namer._

  /**
   * A program is temporally stratifiable if there is no negated dependency
   * that participates in a cycle of dependencies.
   */
  lazy val isTemporallyStratifiable: Program => Boolean =
    attr { program =>
      !program.statements.exists { stmt =>
        participatesInNonTemporalCycle(stmt) && statementDependencies(stmt).exists(_.isNegated)
      }
    }

  lazy val ruleStratum: Statement => Int =
    circular(0) {
      case Statement(lhs, op, rhs) =>
        if (op == BloomOp.<=) {  // deductive rule
          (lhs->collectionDeclaration)->collectionStratum
        } else {  // temporal rule
          -1  // Place in the last stratum
        }
    }

  lazy val collectionStratum: CollectionDeclaration => Int =
    circular(0) { collection =>
      val stmts = collection->collectionStatements
      val nonTemporalDeps = stmts.flatMap(statementDependencies).filterNot(_.isTemporal)
      val depStrata = nonTemporalDeps.map { case Dependency(col, isNegated, _, _) =>
        collectionStratum(col) +  (if (isNegated) 1 else 0)
      }
      (depStrata ++ Set(0)).max
    }
}
