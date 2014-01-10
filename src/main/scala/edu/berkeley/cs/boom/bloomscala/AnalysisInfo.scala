package edu.berkeley.cs.boom.bloomscala

import scala.util.parsing.input.Position
import edu.berkeley.cs.boom.bloomscala.parser._
import scala.collection.mutable
import scalax.collection.{Graph => BaseGraph}
import scalax.collection.mutable.Graph
import scalax.collection.edge.LkDiEdge
import scalax.collection.io.dot._
import com.typesafe.scalalogging.slf4j.Logging

/**
 * Contains data shared across compilation phases
 */
private class AnalysisInfo(val parseResults: List[Either[CollectionDeclaration, Statement]]) {
  val collections = new CollectionInfo
  val ruleGraph = Graph[CollectionDeclaration, LkDiEdge]()

  def graphToDot: String = {
     val dotRoot = DotRootGraph(directed = true, id = None)
     // TODO: it was annoying that I had to use BaseGraph here.
     // I should open a bug report with scala-graph:
     def edgeTransformer(innerEdge: BaseGraph[CollectionDeclaration, LkDiEdge]#EdgeT):
       Option[(DotGraph, DotEdgeStmt)] = {
        val edge = innerEdge.edge
        val label = edge.label.asInstanceOf[Statement]
        val from = edge.from.value.asInstanceOf[CollectionDeclaration].name
        val to = edge.to.value.asInstanceOf[CollectionDeclaration].name
        Some(dotRoot,
            DotEdgeStmt(from, to, List(DotAttr("label", label.pretty))))
      }
     graph2DotExport(ruleGraph).toDot(dotRoot, edgeTransformer)
   }
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