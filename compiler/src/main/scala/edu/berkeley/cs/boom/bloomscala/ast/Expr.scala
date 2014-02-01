package edu.berkeley.cs.boom.bloomscala.ast

import edu.berkeley.cs.boom.bloomscala.typing.{RecordType, BloomType}

/************************* Base Classes ***********************************/

sealed trait Expr extends Node {
  val typ: BloomType
}

/**
 * An expression producing a scalar value.
 */
trait ColExpr extends Expr

/**
 * An expression producing a fixed-size array with heterogeneous element types.
 */
case class RowExpr(cols: List[ColExpr]) extends Expr {
  val typ: BloomType = RecordType(cols.map(_.typ))
}


/*************************** Arithmetic ***********************************/

case class PlusStatement(lhs: ColExpr, rhs: ColExpr, override val typ: BloomType) extends ColExpr


/************************** Predicates ***********************************/

trait Predicate extends Node

case class EqualityPredicate(a: ColExpr, b: ColExpr) extends Predicate