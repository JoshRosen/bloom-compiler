package edu.berkeley.cs.boom.bloomscala.rewriting

import edu.berkeley.cs.boom.bloomscala.parser.AST._
import org.kiama.rewriting.PositionalRewriter._

/**
 * Simple rewrites that are applied before attribute calculation.
 */
object InitialRewrites {

  private val joinMapConsolidation =
    rule {
      case MappedCollection(JoinedCollection(a, b, EqualityPredicate(aExpr, bExpr)), tupleVars, colExprs) =>
        MappedEquijoin(a, b, aExpr, bExpr, tupleVars, colExprs)
    }

  private val collectionRefToTupleVar =
    rule {
      case FreeCollectionRef(name) => FreeTupleVariable(name)
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

  def apply(program: Program): Program =
    rewrite(everywheretd(labelTupleVars) <* everywheretd(joinMapConsolidation))(program)
}
