package edu.berkeley.cs.boom.bloomscala.ast

import edu.berkeley.cs.boom.bloomscala.typing.{CollectionType, FieldType, RecordType}
import CollectionType.CollectionType
import edu.berkeley.cs.boom.bloomscala.typing.FieldType._


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
}

case class FreeCollectionRef(name: String) extends CollectionRef
case class FreeTupleVariable(name: String) extends CollectionRef
case class BoundCollectionRef(name: String, override val collection: CollectionDeclaration) extends CollectionRef


/************************* Fields ******************************************/

case class Field(name: String, typ: FieldType) extends Node

class UnknownField extends Field("$$unknownField", UnknownFieldType)

trait FieldRef extends ColExpr {
  val collection: CollectionRef
  val fieldName: String
  val field: Field = new UnknownField()
}

case class FreeFieldRef(collection: CollectionRef, fieldName: String) extends FieldRef
case class BoundFieldRef(collection: CollectionRef, fieldName: String, override val field: Field) extends FieldRef