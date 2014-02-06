package edu.berkeley.cs.boom.bloomscala.stdlib

import edu.berkeley.cs.boom.bloomscala.typing.{UnknownType, FunctionTypes, FunctionType}

class BuiltInFunction(val name: String, val typ: FunctionType)

/** Placeholder in function reference that we haven't resolved yet */
object UnresolvedFunction extends BuiltInFunction("unresolved", FunctionType(List.empty, UnknownType(), Set.empty))

/** Placeholder in function reference that couldn't be resolved, resulting in a compiler error */
object UnknownFunction extends BuiltInFunction("unknown", FunctionType(List.empty, UnknownType(), Set.empty))

object MinFunction extends BuiltInFunction("min", FunctionTypes.exemplaryAggregate)

object BuiltInFunctions {
  val nameToFunction: Map[String, BuiltInFunction] = Map(
    "min" -> MinFunction
  )
}