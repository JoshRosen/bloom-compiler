package edu.berkeley.cs.boom.bloomscala.parser

import org.kiama.attribution.Attributable
import org.kiama.util.Positioned
import edu.berkeley.cs.boom.bloomscala.typing.FieldType._
import edu.berkeley.cs.boom.bloomscala.parser.AST.BloomOp.BloomOp
import edu.berkeley.cs.boom.bloomscala.parser.AST.CollectionType.CollectionType
import edu.berkeley.cs.boom.bloomscala.typing.RecordType


object AST {
  trait Node extends Attributable with Positioned

  case class Program(nodes: Traversable[Node]) extends Node {
    lazy val declarations: Traversable[CollectionDeclaration] =
      nodes.filter(_.isInstanceOf[CollectionDeclaration]).map(_.asInstanceOf[CollectionDeclaration])
    lazy val statements: Traversable[Statement] =
      nodes.filter(_.isInstanceOf[Statement]).map(_.asInstanceOf[Statement])
  }

  case class Statement(lhs: CollectionRef, op: BloomOp, rhs: StatementRHS, number: Int = -1) extends Node

  case class CollectionDeclaration(
      collectionType: CollectionType,
      name: String,
      keys: List[Field],
      values: List[Field])
    extends Node {
    val schema: RecordType = RecordType((keys ++ values).map(_.typ))
    def getField(name: String): Option[Field] = {
      (keys ++ values).find(_.name == name)
    }
    def indexOfField(name: String): Int = {
      (keys ++ values).indexOf(getField(name).get)
    }
  }

  class MissingDeclaration() extends CollectionDeclaration(CollectionType.Table, "$$UnknownCollection", List.empty, List.empty)

  /** Valid RHS's of statements */
  trait StatementRHS extends Node
  /** Valid targets of the map ({|| []}) operation */
  trait MappedCollectionTarget extends Node
  /** Collections that are derived through operations like map and join */
  trait DerivedCollection extends StatementRHS with MappedCollectionTarget with Node


  case class MappedCollection(collection: MappedCollectionTarget, tupleVars: List[String],
                              rowExpr: RowExpr) extends DerivedCollection
  case class NotIn(a: CollectionRef, b: CollectionRef) extends DerivedCollection
  case class JoinedCollection(a: CollectionRef, b: CollectionRef, predicate: Predicate)
    extends DerivedCollection
  case class MappedEquijoin(a: CollectionRef,
                            b: CollectionRef,
                            aExpr: ColExpr,
                            bExpr: ColExpr,
                            tupleVars: List[String],
                            rowExpr: RowExpr) extends DerivedCollection

  trait CollectionRef extends MappedCollectionTarget with StatementRHS {
    val name: String
    val collection: CollectionDeclaration = new MissingDeclaration()
  }
  case class FreeCollectionRef(name: String) extends CollectionRef
  case class FreeTupleVariable(name: String) extends CollectionRef
  case class BoundCollectionRef(name: String, override val collection: CollectionDeclaration) extends CollectionRef

  case class Field(name: String, typ: FieldType) extends Node
  class UnknownField extends Field("$$unknownField", UnknownFieldType)

  trait Expr extends Node
  trait ColExpr extends Expr
  case class RowExpr(cols: List[ColExpr]) extends Expr

  trait FieldRef extends ColExpr {
    val collection: CollectionRef
    val fieldName: String
    val field: Field = new UnknownField()
  }

  case class FreeFieldRef(collection: CollectionRef, fieldName: String) extends FieldRef
  case class BoundFieldRef(collection: CollectionRef, fieldName: String, override val field: Field) extends FieldRef

  case class PlusStatement(lhs: ColExpr, rhs: ColExpr) extends ColExpr

  trait Predicate extends Node
  case class EqualityPredicate(a: ColExpr, b: ColExpr) extends Predicate

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
}
