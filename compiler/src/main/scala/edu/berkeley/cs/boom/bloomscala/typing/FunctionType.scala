package edu.berkeley.cs.boom.bloomscala.typing


case class FunctionType(argumentTypes: List[BloomType], returnType: BloomType, properties: Set[FunctionProperty]) extends BloomType


object FunctionTypes {
  def leastUpperBound(T: BloomType) = {
    FunctionType(List(T, T), T, FunctionProperties.SemilatticeMerge)
  }

  /**
   * A relation <= that defines a partial ordering of a set.
   */
  def partialOrder(T: BloomType) = {
    FunctionType(List(T, T), FieldType.BloomBoolean, FunctionProperties.PartialOrder)
  }
}