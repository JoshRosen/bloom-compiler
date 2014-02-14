package edu.berkeley.cs.boom.bloomscala.codegen.dataflow

import scala.collection.mutable


case class InputPort(elem: DataflowElement, name: String) {
  elem.inputPorts += this
  val connectedPorts = mutable.HashSet[OutputPort]()
  def <->(outputPort: OutputPort) {
    outputPort.connectedPorts += this
    this.connectedPorts += outputPort
  }
}

case class OutputPort(elem: DataflowElement, name: String) {
  elem.outputPorts += this
  val connectedPorts = mutable.HashSet[InputPort]()
  def <->(inputPort: InputPort) {
    inputPort.connectedPorts += this
    this.connectedPorts += inputPort
  }
}
