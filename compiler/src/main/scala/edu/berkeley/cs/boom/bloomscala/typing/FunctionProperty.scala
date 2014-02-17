package edu.berkeley.cs.boom.bloomscala.typing

sealed trait FunctionProperty

object Pure extends FunctionProperty
object Associative extends FunctionProperty
object Commutative extends FunctionProperty
object Idempotent extends FunctionProperty

/** A binary relation R on a set S is reflexive iff x R x for all x in S.  */
object Reflexive extends FunctionProperty

/** A binary relation R on a set S is antisymmetric iff a R b and b R a implies a == b. */
object Antisymmetric extends FunctionProperty

/** A binary relation R on a set S is transitive iff a R b and b R c implies a R c. */
object Transitive extends FunctionProperty

object FunctionProperties {
  val SemilatticeMerge = Set(Pure, Associative, Commutative, Idempotent)
  val PartialOrder = Set(Pure, Reflexive, Antisymmetric, Transitive)
}