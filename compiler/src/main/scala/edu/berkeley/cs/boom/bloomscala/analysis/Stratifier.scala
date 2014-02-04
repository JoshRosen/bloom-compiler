package edu.berkeley.cs.boom.bloomscala.analysis

import edu.berkeley.cs.boom.bloomscala.ast._
import org.kiama.attribution.Attribution._
import org.kiama.util.Messaging

object Stratum {
  val lastStratum = Stratum(Int.MaxValue)
}

case class Stratum(underlying: Int) extends AnyVal with Ordered[Stratum] {
  def +(delta: Int): Stratum = new Stratum(underlying + delta)
  def compare(that: Stratum): Int = underlying - that.underlying
}

class Stratifier(depAnalyzer: DepAnalyzer)(implicit messaging: Messaging) {

  import depAnalyzer._

  /**
   * A program is temporally stratifiable if there is no negated dependency
   * that participates in a cycle of dependencies.
   */
  lazy val isTemporallyStratifiable: Program => Boolean =
    attr { program =>
      !program.statements.exists { stmt =>
        participatesInDeductiveCycle(stmt) && statementDependencies(stmt).exists(_.isNegated)
      }
    }

  lazy val ruleStratum: Statement => Stratum =
    attr {
      case Statement(lhs, op, rhs, _) =>
        if (op == BloomOp.<=) {  // deductive rule
          lhs.collection->collectionStratum
        } else {  // temporal rule
          Stratum.lastStratum
        }
    }

  lazy val collectionStratum: CollectionDeclaration => Stratum =
    circular(Stratum(0)) { collection =>
      val stmts = collection->collectionStatements
      val nonTemporalDeps = stmts.flatMap(statementDependencies).filterNot(_.isTemporal)
      val depStrata = nonTemporalDeps.map { case Dependency(col, isNegated, _, _) =>
        collectionStratum(col) +  (if (isNegated) 1 else 0)
      }
      (depStrata ++ Set(Stratum(0))).max
    }
}
