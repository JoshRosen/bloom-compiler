package edu.berkeley.cs.boom.bloomscala.typing


trait BloomType

case class FieldType(typeName: String) extends BloomType


object FieldType {
  val BloomInt = new FieldType("int")
  val BloomString = new FieldType("string")
  val UnknownFieldType = new FieldType("unknown")
  val nameToType: Map[String, FieldType] = Map(
    "int" -> BloomInt,
    "string" -> BloomString
  )
}

case class RecordType(fieldTypes: List[FieldType]) extends BloomType