package edu.berkeley.cs.boom.bloomscala.rewriting

import edu.berkeley.cs.boom.bloomscala.ast._
import org.kiama.rewriting.PositionalRewriter._
import java.util.concurrent.atomic.AtomicInteger

/**
 * Simple rewrites that are applied before attribute calculation.
 */
object InitialRewrites {

  private val nextStatementNumber = new AtomicInteger(0)

  private val joinMapConsolidation =
    rule {
      case MappedCollection(JoinedCollection(a, b, EqualityPredicate(aExpr, bExpr)), tupleVars, rowExpr) =>
        MappedEquijoin(a, b, aExpr, bExpr, tupleVars, rowExpr)
    }

  private val collectionRefToTupleVar =
    rule {
      case FreeCollectionRef(name) => FreeTupleVariable(name)
    }

  /**
   * Assign unique numbers to rules.
   */
  private val numberRules =
    rule {
      case Statement(a, b, c, -1) => Statement(a, b, c, nextStatementNumber.getAndIncrement)
    }

  /**
   * If a FreeCollectionRef appears in the body of a MappedCollection statement,
   * change it into a FreeTupleVariable.
   */
  private val labelTupleVars =
    rule {
      case MappedCollection(target, tupleVars, colExprs) =>
        MappedCollection(target, tupleVars,
          rewrite(everywherebu(collectionRefToTupleVar))(colExprs))
    }

  def apply(program: Program): Program = {
    val strategy =
      everywheretd(numberRules) <*
      everywheretd(labelTupleVars) <*
      everywheretd(joinMapConsolidation)
    rewrite(strategy)(program)
  }
}
