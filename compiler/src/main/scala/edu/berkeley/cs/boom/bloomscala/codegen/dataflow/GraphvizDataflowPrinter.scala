package edu.berkeley.cs.boom.bloomscala.codegen.dataflow

import scala.collection.mutable
import edu.berkeley.cs.boom.bloomscala.analysis.Stratum
import edu.berkeley.cs.boom.bloomscala.util.GraphvizPrettyPrinter


/**
 * Translates a dataflow graph to a .dot file that can be viewed using GraphViz.
 */
object GraphvizDataflowPrinter extends DataflowCodeGenerator with GraphvizPrettyPrinter {

  private def label(elem: DataflowElement): String = {
    elem match {
      case Table(collection) =>
        collection.name
      case Scanner(table) =>
        table.collection.name + " Scanner"
      case e: DataflowElement =>
        e.getClass.getSimpleName
    }
  }

  private def shape(elem: DataflowElement): String = {
    elem match {
      case t: Table => "rectangle"
      case e: DataflowElement => "ellipse"
    }
  }

  def generateCode(dataflowGraph: DataflowGraph): CharSequence = {
    // We'll plot each stratum as its own labeled cluster subgraph.
    // Edges that cross between strata represent dependencies between them.

    // Accumulate the GraphViz statements that should go in each cluster subgraph:
    val clusteredStatements = mutable.HashMap[Stratum, mutable.Seq[Doc]]()

    // Edges that cross subgraphs should be declared under the outermost digraph element:
    val topLevelStatements = mutable.Buffer[Doc]()

    for ((stratum, elements) <- dataflowGraph.stratifiedElements) {
      val stratumStatements = mutable.Buffer[Doc]()
      clusteredStatements(stratum) = stratumStatements

      def processPort(outPort: OutputPort) {
        outPort.connectedPorts.foreach { inPort =>
          val edgeCrossesStratum = inPort.elem.stratum != stratum
          val edge = diEdge(outPort.elem.id, inPort.elem.id, "headlabel" -> inPort.name, "taillabel" -> outPort.name,
            "fontsize" -> "8", "arrowsize" -> "0.5")
          if (edgeCrossesStratum) {
            topLevelStatements += edge
          } else {
            stratumStatements += edge
          }
        }
      }

      elements.foreach { e =>
        stratumStatements += node(e.id, "label" -> label(e), "shape" -> shape(e))
        e.outputPorts.foreach(processPort)
      }
    }

    val clusterDot = clusteredStatements.map { case (stratum, dotStatements) =>
      subgraph(s"cluster${stratum.underlying}", s"Stratum ${stratum.underlying}", dotStatements)
    }

    val dot = "digraph" <+> "dataflow" <+> braces(nest(
      linebreak <>
      clusterDot.reduce(_ <@@> _) <@@>
      topLevelStatements.foldLeft(empty)(_ <@@> _)
    ) <> linebreak)
    super.pretty(dot)
  }
}
