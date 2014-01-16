package edu.berkeley.cs.boom.bloomscala.analysis

import org.kiama.attribution.Attribution._
import org.kiama.util.Messaging
import edu.berkeley.cs.boom.bloomscala.parser.AST._
import org.kiama.attribution.Attributable

case class Dependency(dependency: CollectionDeclaration,
                      isNegated: Boolean,
                      isTemporal: Boolean,
                      stmt: Statement)

class DepAnalyzer(messaging: Messaging, namer: Namer) {

  import namer._

  /**
   * The statements defining this collection.
   */
  lazy val collectionStatements: CollectionDeclaration => Set[Statement] =
    attr { colDecl =>
      (colDecl.parent[Program]).statements.filter(stmt => collectionDeclaration(stmt.lhs) == colDecl).toSet
    }

  /**
   * A annotated list of the collections that this statement's RHS depends on.
   */
  lazy val statementDependencies: Statement => Traversable[Dependency] =
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
  lazy val annotatedDependencies: Attributable => Traversable[(CollectionDeclaration, Boolean)] =
    attr {
      case mc: MappedCollection => mc.colExprs.flatMap(annotatedDependencies)
      case NotIn(a, b) => Seq((a, false), (b, true))
      case cr: CollectionRef => Seq((cr, false))
      case a: Attributable => a.children.flatMap(annotatedDependencies).toTraversable
    }

  lazy val transitiveDeductiveDependencies: Statement => Set[Statement] =
    circular(Set.empty[Statement]) {
      stmt =>
        val deps = stmt -> statementDependencies
        val nextCollections = deps.filterNot(_.isTemporal).map(_.dependency).toSet
        val nextStatements = nextCollections.flatMap(collectionStatements)
        nextStatements ++ nextStatements.flatMap(transitiveDeductiveDependencies)
    }

  lazy val participatesInDeductiveCycle: Statement => Boolean =
    attr {
      stmt =>
        transitiveDeductiveDependencies(stmt).contains(stmt)
    }
}
