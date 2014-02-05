package edu.berkeley.cs.boom.bloomscala

import org.scalatest.FunSuite
import edu.berkeley.cs.boom.bloomscala.typing.FieldType._
import edu.berkeley.cs.boom.bloomscala.typing._
import edu.berkeley.cs.boom.bloomscala.typing.FunctionType


class UnifierSuite extends FunSuite {

  test("basic types unify with themselves") {
    assert(Unifier.unify(BloomInt, BloomInt).isSuccess)
  }

  test("unrestricted type parameters unify with each other") {
    assert(Unifier.unify(new TypeParameter("S"), new TypeParameter("T")).isSuccess)
  }

  test("exemplaryAggregate unifies with itself") {
    assert(Unifier.unify(FunctionTypes.exemplaryAggregate, FunctionTypes.exemplaryAggregate).isSuccess)
  }

  test("exemplaryAggregate unifies with intMin") {
    def intMin = FunctionType(List(BloomInt, BloomInt), BloomInt, FunctionProperties.SemilatticeMerge)
    assert(Unifier.unify(FunctionTypes.exemplaryAggregate, intMin).isSuccess)
  }

  test("functions cannot unify with primitives") {
    assert(Unifier.unify(FunctionTypes.exemplaryAggregate, BloomString).isFailure)
    assert(Unifier.unify(BloomString, FunctionTypes.exemplaryAggregate).isFailure)
  }

  test("simple non-unifiable functions") {
    val T = new TypeParameter("T")
    def a = FunctionType(List(T, T), BloomInt, Set.empty)
    def b = FunctionType(List(BloomString, BloomInt), BloomInt, Set.empty)
    assert(Unifier.unify(a, b).isFailure)
  }

  test("functions with different arities should not unify") {
    def a = FunctionType(List.empty, BloomInt, Set.empty)
    def b = FunctionType(List(BloomString, BloomInt), BloomInt, Set.empty)
    assert(Unifier.unify(a, b).isFailure)
  }
}
