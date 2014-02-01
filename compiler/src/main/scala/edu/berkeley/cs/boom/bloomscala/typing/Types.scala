package edu.berkeley.cs.boom.bloomscala.typing

import org.kiama.attribution.Attributable


trait BloomType

/**
 * Placeholder for types that have not been resolved by the typechecker.
 * These will be substituted for the actual types during the typechecking
 * phase.
 */
case class UnboundType() extends BloomType with Attributable

/**
 * Placeholder for types for which type resolution failed.
 */
case class UnknownType() extends BloomType


case class FieldType(typeName: String) extends BloomType


/**
 * Primitive types.
 */
object FieldType {
  val BloomInt = new FieldType("int")
  val BloomString = new FieldType("string")
  val nameToType: Map[String, FieldType] = Map(
    "int" -> BloomInt,
    "string" -> BloomString
  )
}

/**
 * Heterogeneous list / array type.
 */
case class RecordType(fieldTypes: List[BloomType]) extends BloomType