package edu.berkeley.cs.boom.bloomscala.stdlib

import edu.berkeley.cs.boom.bloomscala.typing._
import edu.berkeley.cs.boom.bloomscala.typing.UnknownType
import edu.berkeley.cs.boom.bloomscala.typing.FunctionType

class BuiltInFunction(val name: String, val typ: FunctionType)

/** Placeholder in function reference that we haven't resolved yet */
object UnresolvedFunction extends BuiltInFunction("unresolved", FunctionType(List.empty, UnknownType(), Set.empty))

/** Placeholder in function reference that couldn't be resolved, resulting in a compiler error */
object UnknownFunction extends BuiltInFunction("unknown", FunctionType(List.empty, UnknownType(), Set.empty))

object IntOrder extends BuiltInFunction("intOrder", FunctionTypes.partialOrder(FieldType.BloomInt))
object StringOrder extends BuiltInFunction("stringOrder", FunctionTypes.partialOrder(FieldType.BloomString))


object BuiltInFunctions {
  val nameToFunction: Map[String, BuiltInFunction] = Map(
    "intOrder" -> IntOrder,
    "stringOrder" -> StringOrder
  )
}