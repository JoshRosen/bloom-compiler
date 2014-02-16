package edu.berkeley.cs.boom.bloomscala.analysis

import org.kiama.attribution.Attribution._
import edu.berkeley.cs.boom.bloomscala.ast._
import org.kiama.attribution.Attributable

case class Dependency(dependency: CollectionDeclaration,
                      isNegated: Boolean,
                      isTemporal: Boolean,
                      isMonotonic: Boolean,
                      stmt: Statement)

class DepAnalyzer(program: Program) {

  /**
   * The statements defining this collection.
   */
  lazy val collectionStatements: CollectionDeclaration => Set[Statement] =
    attr { colDecl =>
      program.statements.filter(stmt => stmt.lhs.collection == colDecl).toSet
    }

  /**
   * The collections referenced in this subtree.
   */
  lazy val referencedCollections: Attributable => Set[CollectionDeclaration] =
    attr {
      case cr: CollectionRef => Set(cr.collection)
      case n: Node => n.children.map(referencedCollections).foldLeft(Set.empty[CollectionDeclaration])(_.union(_))
    }

  /**
   * The collection columns referenced in this subtree.
   */
  lazy val referencedColumns: Attributable => Set[FieldRef] =
    attr {
      case fr: FieldRef => Set(fr)
      case n: Node => n.children.map(referencedColumns).foldLeft(Set.empty[FieldRef])(_.union(_))
    }

  lazy val dependentStatements: CollectionDeclaration => Set[Statement] =
    attr { cd =>
      program.statements.filter(referencedCollections(_).contains(cd)).toSet
    }

  /**
   * A annotated list of the collections that this statement's RHS depends on.
   */
  lazy val statementDependencies: Statement => Traversable[Dependency] =
    attr {
      case stmt @ Statement(lhs, op, rhs, _) =>
        val isTemporal = stmt.op != BloomOp.<=
        for ((collection, isNegated, isMonotonic) <- annotatedDependencies(rhs)) yield {
          Dependency(collection, isNegated, isTemporal, isMonotonic, stmt)
        }
    }

  /**
   * Helper attribute that annotates referenced collections based on whether the reference
   * is through negation or appears in a non-monotonic position.
   */
  lazy val annotatedDependencies: Attributable => Traversable[(CollectionDeclaration, Boolean, Boolean)] =
    // TODO: refactor the return type to use case classes instead of tuples.
    attr {
      case mc: MappedCollection => mc.rowExpr.cols.flatMap(annotatedDependencies)
      case NotIn(a, b) => Seq((a.collection, false, true), (b.collection, true, false))
      case cr: CollectionRef => Seq((cr.collection, false, true))
      case choose: ChooseCollection => Seq((choose.collection.collection, false, false))
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
