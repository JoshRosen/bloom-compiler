package edu.berkeley.cs.boom.bloomscala.ast

import edu.berkeley.cs.boom.bloomscala.typing._
import edu.berkeley.cs.boom.bloomscala.typing.CollectionType
import edu.berkeley.cs.boom.bloomscala.typing.CollectionType.CollectionType
import edu.berkeley.cs.boom.bloomscala.typing.RecordType
import edu.berkeley.cs.boom.bloomscala.typing.UnknownType


/************************* Collections ************************************/

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

class MissingDeclaration() extends CollectionDeclaration(CollectionType.Table,
  "$$UnknownCollection", List.empty, List.empty)


trait CollectionRef extends MappedCollectionTarget with StatementRHS {
  val name: String
  val collection: CollectionDeclaration = new MissingDeclaration()
  /**
   * If a collection is referenced inside of a lambda, this determines
   * which of the lambda's positional arguments this reference should be
   * bound to.  This is 0-indexed, and is -1 if it's not applicable
   * or if this collection reference could not be resolved.
   */
  val lambdaArgNumber: Int = -1
}

case class FreeCollectionRef(name: String) extends CollectionRef
case class FreeTupleVariable(name: String) extends CollectionRef
case class BoundCollectionRef(
    name: String,
    override val collection: CollectionDeclaration,
    override val lambdaArgNumber: Int
 ) extends CollectionRef


/************************* Fields ******************************************/

case class Field(name: String, typ: BloomType) extends Node

class UnknownField extends Field("$$unknownField", UnknownType())

trait FieldRef extends ColExpr {
  val collection: CollectionRef
  val fieldName: String
  val field: Field = new UnknownField()
  val typ: BloomType = field.typ
}

case class FreeFieldRef(collection: CollectionRef, fieldName: String) extends FieldRef
case class BoundFieldRef(collection: CollectionRef, fieldName: String, override val field: Field) extends FieldRef