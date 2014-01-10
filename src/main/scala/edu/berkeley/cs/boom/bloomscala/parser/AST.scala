package edu.berkeley.cs.boom.bloomscala.parser

import scala.util.parsing.input.Positional
import edu.berkeley.cs.boom.bloomscala.parser.CollectionType.CollectionType
import edu.berkeley.cs.boom.bloomscala.parser.FieldType.FieldType
import edu.berkeley.cs.boom.bloomscala.parser.BloomOp.BloomOp


case class CollectionDeclaration(
    collectionType: CollectionType,
    name: String,
    keys: List[Field],
    values: List[Field])
  extends Positional {
  val schema: List[FieldType.FieldType] = (keys ++ values).map(_.typ)
  def getField(name: String): Option[Field] = {
    (keys ++ values).find(_.name == name)
  }
}

/** Valid RHS's of statements */
sealed trait StatementRHS

/** Valid targets of the map ({|| []}) operation */
sealed trait MappedCollectionTarget

/** Collections that are derived through operations like map and join */
sealed trait DerivedCollection extends StatementRHS with MappedCollectionTarget

case class MappedCollection(collection: MappedCollectionTarget, shortNames: List[String], colExprs: List[ColExpr]) extends DerivedCollection with Positional {
  var schema: Option[List[FieldType]] = None
}
case class JoinedCollection(a: CollectionRef, b: CollectionRef, predicate: Any) extends DerivedCollection

case class CollectionRef(name: String) extends MappedCollectionTarget with Positional
case class Field(name: String, typ: FieldType) extends Positional
case class FieldRef(collectionName: String, fieldName: String) extends ColExpr

abstract class ColExpr extends Positional {
  /** Set during typechecking */
  var typ: Option[FieldType] = None
}
case class PlusStatement(lhs: ColExpr, rhs: ColExpr) extends ColExpr

case class Statement(lhs: Any, op: BloomOp, rhs: StatementRHS) extends Positional

case class EqualityPredicate[T](a: T, b: T) extends Positional

object BloomOp extends Enumeration {
  type BloomOp = Value
  val InstantaneousMerge, DeferredMerge, AsynchronousMerge, Delete, DeferredUpdate = Value
  val symbolToOp: Map[String, BloomOp] = Map(
    "<=" -> InstantaneousMerge,
    "<+" -> DeferredMerge,
    "<~" -> AsynchronousMerge,
    "<-" -> Delete,
    "<+-" -> DeferredUpdate
  )
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