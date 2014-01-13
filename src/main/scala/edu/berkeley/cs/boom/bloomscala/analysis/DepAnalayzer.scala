package edu.berkeley.cs.boom.bloomscala.analysis

import org.kiama.attribution.Attribution._
import org.kiama.util.Messaging
import edu.berkeley.cs.boom.bloomscala.parser.AST._
import scala.collection.GenTraversable
import org.kiama.attribution.Attributable

case class Dependency(dependency: CollectionDeclaration,
                      isNegated: Boolean,
                      isTemporal: Boolean,
                      stmt: Statement)

class DepAnalayzer(messaging: Messaging, namer: Namer) {

  import namer._

  /**
   * This defines the edges of the precedence graph.
   */
  lazy val statementDependencies: Statement => GenTraversable[Dependency] =
    attr {
      case stmt @ Statement(lhs, op, rhs) =>
        val isTemporal = stmt.op != BloomOp.<=
        for ((collection, isNegated) <- annotatedDependencies(rhs)) yield {
          Dependency(collection, isNegated, isTemporal, stmt)
        }
    }

  /**
   * Helper attribute that annotates referenced collections based on whether the reference
   * is through negation.
   */
  lazy val annotatedDependencies: Attributable => GenTraversable[(CollectionDeclaration, Boolean)] =
    attr {
      case stmt @ Statement(lhs, op, rhs) => annotatedDependencies(rhs)
      case mc: MappedCollection => mc.colExprs.flatMap(annotatedDependencies)
      case NotIn(a, b) =>
        Seq((a->collectionDeclaration, false), (b->collectionDeclaration, true))
      case cr: CollectionRef =>
        Seq((cr->collectionDeclaration, false))
      case a: Attributable => a.children.flatMap(annotatedDependencies).toTraversable
    }

  lazy val participatesInNonTemporalCycle: Statement => Boolean =
    circular(true) { case stmt =>
      val deps: GenTraversable[Dependency] = stmt->statementDependencies
      val nextHops = deps.filterNot(_.isTemporal)
      nextHops.map(d => participatesInNonTemporalCycle(d.stmt)).foldLeft(false)(_||_)
    }
}
