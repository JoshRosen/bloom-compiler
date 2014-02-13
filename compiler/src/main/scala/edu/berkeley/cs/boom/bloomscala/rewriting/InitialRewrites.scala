package edu.berkeley.cs.boom.bloomscala.rewriting

import edu.berkeley.cs.boom.bloomscala.ast._
import org.kiama.rewriting.PositionalRewriter._
import java.util.concurrent.atomic.AtomicInteger

/**
 * Simple rewrites that are applied before attribute calculation.
 */
object InitialRewrites {

  private val nextStatementNumber = new AtomicInteger(0)

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
      case JoinedCollections(collections, predicate, tupleVars, rowExpr) =>
        JoinedCollections(collections, predicate, tupleVars,
          rewrite(everywherebu(collectionRefToTupleVar))(rowExpr) )
      case MappedCollection(target, tupleVars, rowExpr) =>
        MappedCollection(target, tupleVars,
          rewrite(everywherebu(collectionRefToTupleVar))(rowExpr))
    }

  def apply(program: Program): Program = {
    val strategy =
      everywheretd(numberRules) <*
      everywheretd(labelTupleVars)
    rewrite(strategy)(program)
  }
}
