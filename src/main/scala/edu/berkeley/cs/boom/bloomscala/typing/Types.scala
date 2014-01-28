package edu.berkeley.cs.boom.bloomscala.typing


trait BloomType


object FieldType extends Enumeration with BloomType {
  type FieldType = Value
  val BloomInt, BloomString, UnknownFieldType = Value
  val nameToType: Map[String, FieldType] = Map(
    "int" -> BloomInt,
    "string" -> BloomString
  )
}

case class RecordType(fieldTypes: List[FieldType.FieldType]) extends BloomType