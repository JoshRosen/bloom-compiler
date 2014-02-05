package edu.berkeley.cs.boom.bloomscala.typing

sealed trait FunctionProperty

object Pure extends FunctionProperty
object Associative extends FunctionProperty
object Commutative extends FunctionProperty
object Idempotent extends FunctionProperty

object FunctionProperties {
  val SemilatticeMerge = Set(Pure, Associative, Commutative, Idempotent)
}