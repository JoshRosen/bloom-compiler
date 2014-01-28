package edu.berkeley.cs.boom.bloomscala.ast


case class Program(nodes: Traversable[Node]) extends Node {
  lazy val declarations: Traversable[CollectionDeclaration] =
    nodes.filter(_.isInstanceOf[CollectionDeclaration]).map(_.asInstanceOf[CollectionDeclaration])
  lazy val statements: Traversable[Statement] =
    nodes.filter(_.isInstanceOf[Statement]).map(_.asInstanceOf[Statement])
}