package edu.berkeley.cs.boom.bloomscala.rewriting

import edu.berkeley.cs.boom.bloomscala.parser.AST._

object DeltaForm {

  def delta(cr: CollectionRef): DeltaCollectionRef = {
    DeltaCollectionRef(cr.name, cr.collection)
  }

  /**
   * Maps a given rule into one or more rules accepting deltas of the collections
   * referenced in the RHS.  The result is a list of (deltaCollection, deltaRule)
   * pairs.
   */
  def toDeltaForm(stmt: Statement): Set[(CollectionRef, Statement)] = {
    stmt match { case Statement(lhs, op, rhs) =>
      val rhsDeltaRules =
        rhs match {
          // TODO: we probably only need to special-case for binary operations, where we may
          // need to generate multiple delta rules.  For unary operators, we can just replace the
          // first occurrence of a CollectionRef found in a top-down, left-to-right tree traversal.
          case cr: CollectionRef => Set((cr, delta(cr)))
          case MappedCollection(r: CollectionRef, sn, ce) =>  Set((r, MappedCollection(delta(r), sn, ce)))
          case MappedEquijoin(a, b, aExpr, bExpr, shortNames, colExprs) =>
            Set(
              // TODO: what about self-joins of the deltas?
              (a, MappedEquijoin(delta(a), b, aExpr, bExpr, shortNames, colExprs)),
              (b, MappedEquijoin(a, delta(b), aExpr, bExpr, shortNames, colExprs))
            )
        }
      rhsDeltaRules.map(newRhs => (newRhs._1, Statement(delta(lhs), op, newRhs._2)))
    }
  }
}
