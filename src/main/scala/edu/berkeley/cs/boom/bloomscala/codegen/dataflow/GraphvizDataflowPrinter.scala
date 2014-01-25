package edu.berkeley.cs.boom.bloomscala.codegen.dataflow

import scala.collection.mutable


/**
 * Translates a dataflow graph to a .dot file that can be
 * viewed using GraphViz.
 *
 * To make the graph easier to read, tables are displayed twice as
 * separate source and sink nodes.
 */
object GraphvizDataflowPrinter extends DataflowCodeGenerator {

  private def label(elem: DataflowElement): String = {
    elem match {
      case Table(collection) =>
        collection.name
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
    val visited = mutable.HashSet[DataflowElement]()
    val edges = mutable.Buffer[Doc]()
    def visit(elem: DataflowElement) {
      if (visited.add(elem)) {
        def processPort(outPort: OutputPort) {
          edges ++= outPort.connectedPorts.map { inPort =>
            val sink= inPort.elem match {
              case t: Table => "sink"
              case _ => ""
            }
            text(s"""${outPort.elem.id} -> $sink${inPort.elem.id} [headlabel="${inPort.name}",taillabel="${outPort.name}",fontsize=8,arrowsize=0.5];""")
          }
        }
        processPort(elem.deltaOut)
        processPort(elem.deleteOut)
        (elem.deleteOut.connectedPorts ++ elem.deltaOut.connectedPorts).map(_.elem).foreach(visit)
      }
    }
    dataflowGraph.elements.foreach(visit)
    val nodes = visited.map { elem =>
      text(s"""${elem.id} [label="${label(elem)}",shape="${shape(elem)}"];""")
    } ++ dataflowGraph.tables.values.filter(_.hasInputs).map { elem =>
      text(s"""sink${elem.id} [label="${label(elem)}",shape="${shape(elem)}"];""")
    }
    val dot = "digraph" <+> "dataflow" <+> braces(nest(
      linebreak <>
      nodes.reduce(_ <@@> _) <@@>
      edges.reduce(_ <@@> _)
    ) <> linebreak)
    super.pretty(dot)
  }
}
