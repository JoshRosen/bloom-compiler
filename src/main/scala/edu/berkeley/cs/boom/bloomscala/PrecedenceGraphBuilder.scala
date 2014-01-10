package edu.berkeley.cs.boom.bloomscala

import com.typesafe.scalalogging.slf4j.Logging
import edu.berkeley.cs.boom.bloomscala.parser._
import scalax.collection.{Graph => BaseGraph}
import scalax.collection.mutable.Graph
import scalax.collection.edge.LkDiEdge
import scalax.collection.io.dot._
import scala.Some


/**
 * Constructs the precedence graph, whose nodes are rules/predicates/collections
 * and whose directed edges point to dependent rules.
 *
 * Edges are labeled according to whether the dependency is negated and whether
 * that negation is temporal.
 */
class PrecedenceGraphBuilder(analysisInfo: AnalysisInfo) extends Logging {

  /**
   * For simplicity, this is a multigraph.  To make it into a regular digraph,
   * we'd have to perform rewriting to combine/union rule bodies with the same head.
   */
  private val graph = Graph[CollectionDeclaration, LkDiEdge]()

  private case class EdgeLabel(isNegated: Boolean, isTemporal: Boolean, rule: Statement) {
    override def toString: String = (if(isNegated) "negated " else "") + BloomOp.opToSymbol(rule.op)
  }

  private def graphToDot: String = {
    val dotRoot = DotRootGraph(directed = true, id = None)
    // TODO: it was annoying that I had to use BaseGraph here.
    // I should open a bug report with scala-graph:
    def edgeTransformer(innerEdge: BaseGraph[CollectionDeclaration, LkDiEdge]#EdgeT):
    Option[(DotGraph, DotEdgeStmt)] = {
      val edge = innerEdge.edge
      val label = edge.label.asInstanceOf[EdgeLabel]
      val from = edge.from.value.asInstanceOf[CollectionDeclaration].name
      val to = edge.to.value.asInstanceOf[CollectionDeclaration].name
      Some(dotRoot,
        DotEdgeStmt(from, to, List(DotAttr("label", label.toString))))
    }
    graph2DotExport(graph).toDot(dotRoot, edgeTransformer)
  }

  private def buildPrecedenceGraph() {
    // The nodes of the graph are IDB predicates (i.e. collections):
    analysisInfo.collections.foreach(graph.add)
    // Get the rules:
    val statements = analysisInfo.parseResults.filter(_.isRight).map(_.right.get)
    statements.foreach { stmt =>
      val lhs = analysisInfo.collections(stmt.lhs)
      val dependencies = stmt.rhs.getDependencies(analysisInfo)
      val isTemporal = stmt.op != BloomOp.<=
      dependencies.foreach { case (collection, isNegated) =>
        graph.addLEdge(collection, lhs)(EdgeLabel(isNegated, isTemporal, stmt))(LkDiEdge)
      }
    }
  }

  /**
   * Test whether the program is temporally stratifiable.  A program is temporally
   * unstratifiable if its precedence graph contains a cycle with non-temporal negation.
   */
  //private def isTemporallyStratifiable(): Boolean = ???


  def run() {
    buildPrecedenceGraph()
    logger.debug(s"Precedence Graph:\n$graphToDot")
    // Stratify the rules.

    // If the rule body doesn't reference any collections, it won't be
    // assigned a stratum, so just place it in stratum zero.

    // All temporal rules are placed in the last stratum.
  }
}
