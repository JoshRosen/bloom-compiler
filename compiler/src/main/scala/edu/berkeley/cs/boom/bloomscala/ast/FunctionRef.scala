package edu.berkeley.cs.boom.bloomscala.ast

import edu.berkeley.cs.boom.bloomscala.stdlib.{UnresolvedFunction, BuiltInFunction}


trait FunctionRef extends Node {
  val name: String
  val function: BuiltInFunction = UnresolvedFunction
}

case class FreeFunctionRef(name: String) extends FunctionRef
case class BoundFunctionRef(name: String, override val function: BuiltInFunction) extends FunctionRef