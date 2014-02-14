package edu.berkeley.cs.boom.bloomscala.codegen.dataflow

import scala.collection.mutable
import java.util.concurrent.atomic.AtomicInteger

object Edge {
  private val nextEdgeId = new AtomicInteger(0)
}

case class Edge(from: OutputPort, to: InputPort) {
  val id = Edge.nextEdgeId.getAndIncrement

  override def equals(other: Any): Boolean = other match {
    case that: DataflowElement => id == that.id
    case _ => false
  }

  override def hashCode(): Int = id
}


case class InputPort(elem: DataflowElement, name: String) {
  elem.inputPorts += this
  val connections = mutable.HashSet[Edge]()
  def <->(outputPort: OutputPort) {
    val edge = Edge(outputPort, this)
    outputPort.connections += edge
    this.connections += edge
  }
}

case class OutputPort(elem: DataflowElement, name: String) {
  elem.outputPorts += this
  val connections = mutable.HashSet[Edge]()
  def <->(inputPort: InputPort) {
    val edge = Edge(this, inputPort)
    inputPort.connections += edge
    this.connections += edge
  }
}