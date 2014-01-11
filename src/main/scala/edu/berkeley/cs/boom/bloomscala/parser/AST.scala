package edu.berkeley.cs.boom.bloomscala.parser

import scala.util.parsing.input.Positional
import edu.berkeley.cs.boom.bloomscala.parser.CollectionType.CollectionType
import edu.berkeley.cs.boom.bloomscala.parser.FieldType.FieldType
import edu.berkeley.cs.boom.bloomscala.parser.BloomOp.BloomOp
import edu.berkeley.cs.boom.bloomscala.AnalysisInfo

trait Pretty {
  def pretty: String = toString
}

sealed trait DepAnalysis {
  /**
   * Return the set of collections that this rule depends on, with each
   * collection marked according to whether the dependency is negated.
   */
  def getDependencies(analysisInfo: AnalysisInfo): Set[(CollectionDeclaration, Boolean)]
}

case class CollectionDeclaration(
    collectionType: CollectionType,
    name: String,
    keys: List[Field],
    values: List[Field])
  extends Positional {
  /** Set by the Stratifier */
  var stratum: Int = 0
  /** Used by the Stratifier to track its graph traversal status */
  var stratificationStatus = "init"
  /**
   * Modified by the Stratifier to record whether this node is reachable
   * via a path in the precedence graph that contains a negated edge.
   */
  var reachableViaPathWithNegation = false
  val schema: List[FieldType.FieldType] = (keys ++ values).map(_.typ)
  def getField(name: String): Option[Field] = {
    (keys ++ values).find(_.name == name)
  }
}

/** Valid RHS's of statements */
sealed trait StatementRHS extends DepAnalysis with Pretty

/** Valid targets of the map ({|| []}) operation */
sealed trait MappedCollectionTarget extends Pretty

/** Collections that are derived through operations like map and join */
sealed trait DerivedCollection extends StatementRHS with MappedCollectionTarget

case class MappedCollection(collection: MappedCollectionTarget, shortNames: List[String],
                            colExprs: List[ColExpr]) extends DerivedCollection with Positional {
  var schema: Option[List[FieldType]] = None
  def getDependencies(analysisInfo: AnalysisInfo): Set[(CollectionDeclaration, Boolean)] = {
    colExprs.flatMap(_.getDependencies(analysisInfo)).toSet
  }
  override def pretty: String = s"${collection.pretty} { ... }"
}

case class NotIn(a: CollectionRef, b: CollectionRef) extends DerivedCollection with Positional {
  def getDependencies(analysisInfo: AnalysisInfo): Set[(CollectionDeclaration, Boolean)] = {
    Set((analysisInfo.collections(a), false), (analysisInfo.collections(b), true))
  }
}

case class JoinedCollection(a: CollectionRef, b: CollectionRef, predicate: Predicate) extends DerivedCollection {
  def getDependencies(analysisInfo: AnalysisInfo): Set[(CollectionDeclaration, Boolean)] = {
    Set((analysisInfo.collections(a), false), (analysisInfo.collections(b), false))
  }
  override def pretty: String = s"(${a.pretty} * ${b.pretty}) on (${predicate.pretty})"
}

case class CollectionRef(name: String) extends MappedCollectionTarget with Positional with StatementRHS {
  override def pretty: String = name

  def getDependencies(analysisInfo: AnalysisInfo): Set[(CollectionDeclaration, Boolean)] =
    Set((analysisInfo.collections(this), false))

  def collection(implicit analysisInfo: AnalysisInfo): CollectionDeclaration =
    analysisInfo.collections(this)

}
case class Field(name: String, typ: FieldType) extends Positional

// If `collectionName` is an alias, it's expanded during the typechecking phase.
case class FieldRef(var collectionName: String, fieldName: String) extends ColExpr {
  def getDependencies(analysisInfo: AnalysisInfo): Set[(CollectionDeclaration, Boolean)] = {
    Set((collection(analysisInfo), false))
  }

  def collection(implicit analysisInfo: AnalysisInfo): CollectionDeclaration = {
    analysisInfo.collections(collectionName)(this.pos)
  }
}

abstract class ColExpr extends Positional {
  /** Set during typechecking */
  var typ: Option[FieldType] = None
  def getDependencies(analysisInfo: AnalysisInfo): Set[(CollectionDeclaration, Boolean)]
}

case class PlusStatement(lhs: ColExpr, rhs: ColExpr) extends ColExpr {
  def getDependencies(analysisInfo: AnalysisInfo): Set[(CollectionDeclaration, Boolean)] =
    lhs.getDependencies(analysisInfo) ++ rhs.getDependencies(analysisInfo)
}

case class Statement(lhs: CollectionRef, op: BloomOp, rhs: StatementRHS) extends Positional {
  def pretty: String = s"${lhs.pretty} ${BloomOp.opToSymbol(op)} ${rhs.pretty}"
}

trait Predicate extends Pretty

case class EqualityPredicate[T](a: T, b: T) extends Predicate with Positional {
  override def pretty: String = s"$a == $b"
}

object BloomOp extends Enumeration {
  type BloomOp = Value
  val InstantaneousMerge, DeferredMerge, AsynchronousMerge, Delete, DeferredUpdate = Value
  val <= = InstantaneousMerge
  val symbolToOp: Map[String, BloomOp] = Map(
    "<=" -> InstantaneousMerge,
    "<+" -> DeferredMerge,
    "<~" -> AsynchronousMerge,
    "<-" -> Delete,
    "<+-" -> DeferredUpdate
  )
  val opToSymbol = symbolToOp.map(_.swap)
}

object CollectionType extends Enumeration {
  type CollectionType = Value
  val Table, Scratch = Value
  val nameToType: Map[String, CollectionType] = Map(
    "table" -> Table,
    "scratch" -> Scratch
  )
}

object FieldType extends Enumeration {
  type FieldType = Value
  val BloomInt, BloomString = Value
  val nameToType: Map[String, FieldType] = Map(
    "int" -> BloomInt,
    "string" -> BloomString
  )
}