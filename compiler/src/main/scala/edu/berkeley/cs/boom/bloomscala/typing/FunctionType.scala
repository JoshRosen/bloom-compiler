package edu.berkeley.cs.boom.bloomscala.typing


case class FunctionType(argumentTypes: List[BloomType], returnType: BloomType, properties: Set[FunctionProperty]) extends BloomType


object FunctionTypes {
  def exemplaryAggregate = {
    val T = new TypeParameter("T")
    FunctionType(List(T, T), T, FunctionProperties.SemilatticeMerge)
  }
}