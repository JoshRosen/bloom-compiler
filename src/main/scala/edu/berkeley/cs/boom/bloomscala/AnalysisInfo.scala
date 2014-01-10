package edu.berkeley.cs.boom.bloomscala

import scala.util.parsing.input.Position
import edu.berkeley.cs.boom.bloomscala.parser._
import scala.collection.mutable

import com.typesafe.scalalogging.slf4j.Logging

/**
 * Contains data shared across compilation phases
 */
private class AnalysisInfo(val parseResults: List[Either[CollectionDeclaration, Statement]]) {
  val collections = new CollectionInfo

}

/**
 * Helper class for managing declarations of collections and aliasing
 * of collection names in map and join.
 */
private class CollectionInfo extends CompilerUtils with Iterable[CollectionDeclaration] with Logging {
  private val declarations = new mutable.HashMap[String, CollectionDeclaration]
  def declare(decl: CollectionDeclaration) {
    alias(decl.name, decl)
  }
  def alias(name: String, decl: CollectionDeclaration) {
    logger.debug(s"Aliasing ${decl.name} as $name")
    declare(name, decl)
  }
  def declare(name: String, decl: CollectionDeclaration) {
    declarations.get(name) match {
      case Some(existingDeclaration) =>
        val pos = decl.pos
        val firstPos = existingDeclaration.pos
        fail(s"Collection $name declared twice;" +
             s" first declared at ${firstPos.line}:${firstPos.column}:\n${firstPos.longString}" +
             s"\nand re-declared at ${pos.line}:${pos.column}")(decl.pos)
      case None =>
        declarations(name) = decl
    }
  }
  def apply(collectionRef: CollectionRef): CollectionDeclaration = {
    apply(collectionRef.name)(collectionRef.pos)
  }
  def apply(name: String)(implicit pos: Position): CollectionDeclaration = {
    declarations.get(name) match {
      case Some(decl) => decl
      case None =>
        fail(s"Collection $name isn't declared")
    }
  }
  def toMap = declarations.toMap
  override def clone: CollectionInfo = {
    val cloned = new CollectionInfo
    cloned.declarations ++= this.declarations
    cloned
  }

  def iterator: Iterator[CollectionDeclaration] = declarations.valuesIterator
}