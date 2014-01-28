package edu.berkeley.cs.boom.bloomscala.ast

/************************* Base Classes ***********************************/

trait Expr extends Node

/**
 * An expression producing a scalar value.
 */
trait ColExpr extends Expr

/**
 * An expression producing an array.
 */
case class RowExpr(cols: List[ColExpr]) extends Expr


/*************************** Arithmetic ***********************************/

case class PlusStatement(lhs: ColExpr, rhs: ColExpr) extends ColExpr


/************************** Predicates ***********************************/

trait Predicate extends Node

case class EqualityPredicate(a: ColExpr, b: ColExpr) extends Predicate