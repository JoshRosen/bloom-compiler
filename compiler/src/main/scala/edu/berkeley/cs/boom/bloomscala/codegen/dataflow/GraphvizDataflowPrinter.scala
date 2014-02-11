package edu.berkeley.cs.boom.bloomscala.codegen.dataflow

import scala.collection.mutable
import edu.berkeley.cs.boom.bloomscala.analysis.Stratum


/**
 * Translates a dataflow graph to a .dot file that can be
 * viewed using GraphViz.
 *
 * To make the graph easier to read, tables are displayed twice as
 * separate source and sink nodes.
 */
object GraphvizDataflowPrinter extends DataflowCodeGenerator {

  private def label(elem: DataflowElementBase): String = {
    elem match {
      case Table(collection) =>
        collection.name
      case e: DataflowElementBase =>
        e.getClass.getSimpleName
    }
  }

  private def shape(elem: DataflowElementBase): String = {
    elem match {
      case t: Table => "rectangle"
      case e: DataflowElementBase => "ellipse"
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

      val tables = elements.filter(_.isInstanceOf[Table]).map(_.asInstanceOf[Table])
      val nonTables = elements.filterNot(_.isInstanceOf[Table])

      def processPort(outPort: OutputPort) {
        outPort.connectedPorts.foreach { inPort =>
          val edgeCrossesStratum = inPort.elem.stratum != stratum
          val sink = inPort.elem match {
            case t: Table => "sink"
            case _ => ""
          }
          val edge = text(s"""${outPort.elem.id} -> $sink${inPort.elem.id} [headlabel="${inPort.name}",taillabel="${outPort.name}",fontsize=8,arrowsize=0.5];""")
          if (edgeCrossesStratum) {
            topLevelStatements += edge
          } else {
            stratumStatements += edge
          }
        }
      }

      // To make the graph easier to read, non-constant tables are displayed
      // as separate source and sink nodes:
      tables.foreach { t =>
        stratumStatements += text(s"""${t.id} [label="${label(t)}",shape="${shape(t)}"];""")
        if (t.hasInputs) {
          stratumStatements += text(s"""sink${t.id} [label="${label(t)}",shape="${shape(t)}"];""")
        }
        t.outputPorts.foreach(processPort)
      }

      nonTables.foreach { e =>
        stratumStatements += text(s"""${e.id} [label="${label(e)}",shape="${shape(e)}"];""")
        e.outputPorts.foreach(processPort)
      }
    }

    val clusterDot = clusteredStatements.map { case (stratum, dotStatements) =>
      "subgraph" <+> s"cluster${stratum.underlying}" <+> braces(nest(
        linebreak <>
        s"""label="Stratum ${stratum.underlying}";""" <@@>
        dotStatements.reduce(_ <@@> _)
      ) <> linebreak)
    }

    val dot = "digraph" <+> "dataflow" <+> braces(nest(
      linebreak <>
      clusterDot.reduce(_ <@@> _) <@@>
      topLevelStatements.foldLeft(empty)(_ <@@> _)
    ) <> linebreak)
    super.pretty(dot)
  }
}
