package edu.berkeley.cs.boom.bloomscala

import com.typesafe.scalalogging.slf4j.Logging
import edu.berkeley.cs.boom.bloomscala.parser._
import scalax.collection.edge.LkDiEdge


class RuleGraphBuilder(analysisInfo: AnalysisInfo) extends Logging {
  val graph = analysisInfo.ruleGraph

  def run() {
    analysisInfo.collections.foreach(graph.add)
    val statements = analysisInfo.parseResults.filter(_.isRight).map(_.right.get)
    statements.foreach { stmt =>
      val target = analysisInfo.collections(stmt.lhs)
      val dependencies = stmt.rhs.getDependencies(analysisInfo)
      dependencies.foreach(graph.addLEdge(_, target)(stmt)(LkDiEdge))
    }
    logger.debug(s"Rule dependency graph:\n${analysisInfo.graphToDot}")

  }
}
