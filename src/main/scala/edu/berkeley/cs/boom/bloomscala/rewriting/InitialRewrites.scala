package edu.berkeley.cs.boom.bloomscala.rewriting

import edu.berkeley.cs.boom.bloomscala.parser.AST._
import org.kiama.rewriting.Rewriter._

/**
 * Simple rewrites that are applied before attribute calculation.
 */
object InitialRewrites {

  private val joinMapConsolidation =
    rule {
      case MappedCollection(JoinedCollection(a, b, EqualityPredicate(aExpr, bExpr)), shortNames, colExprs) =>
        MappedEquijoin(a, b, aExpr, bExpr, shortNames, colExprs)
    }

  def apply(program: Program): Program =
    rewrite(outermost(joinMapConsolidation))(program)
}
