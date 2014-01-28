package edu.berkeley.cs.boom.bloomscala.codegen.dataflow

import scala.collection.mutable
import edu.berkeley.cs.boom.bloomscala.parser.AST.{RowExpr, ColExpr, CollectionDeclaration}
import java.util.concurrent.atomic.AtomicInteger
import edu.berkeley.cs.boom.bloomscala.analysis.{Stratifier, Stratum}


class DataflowGraph(stratifier: Stratifier) {
  val nextElementId = new AtomicInteger(0)
  val elements = mutable.HashSet[DataflowElement]()  // Not thread-safe (doesn't matter for now)
  def stratifiedElements: Seq[(Stratum, mutable.Set[DataflowElement])] = {
    elements.groupBy(_.stratum).toSeq.sortBy(_._1)
  }
  val tables: mutable.Map[CollectionDeclaration, Table] =
    mutable.HashMap[CollectionDeclaration, Table]().withDefault { decl =>
      val table = Table(decl)(this, stratifier.collectionStratum(decl))
      tables(decl) = table
      elements += table
      table
    }
}

case class InputPort(elem: DataflowElement, name: String) {
  val connectedPorts = mutable.HashSet[OutputPort]()
  def <->(outputPort: OutputPort) {
    outputPort.connectedPorts += this
    this.connectedPorts += outputPort
  }
}

case class OutputPort(elem: DataflowElement, name: String) {
  val connectedPorts = mutable.HashSet[InputPort]()
  def <->(inputPort: InputPort) {
    inputPort.connectedPorts += this
    this.connectedPorts += inputPort
  }
}

/**
 * Represents a generic push-based dataflow element.
 */
class DataflowElement(implicit graph: DataflowGraph, implicit val stratum: Stratum) {
  /** A unique identifier for this dataflow element */
  val id = graph.nextElementId.getAndIncrement

  // This statement needs to be AFTER we assign the id so that hashCode()
  // and equals() return the right results when we add this element to the
  // hashSet:
  graph.elements += this

  /** Insertions produced by this operator */
  val deltaOut = OutputPort(this, "deltaOut")
  /* Deletions produced by this operator */
  val deleteOut = OutputPort(this, "deleteOut")

  override def equals(other: Any): Boolean = other match {
    case that: DataflowElement => id == that.id
    case _ => false
  }

  override def hashCode(): Int = id
}

case class Table(collection: CollectionDeclaration)(implicit g: DataflowGraph, s: Stratum) extends DataflowElement {
  /** Sources of new tuples to process in the current tick */
  val deltaIn = InputPort(this, "dataIn")
  /* Sources of tuples to be added in the next tick */
  val pendingIn = InputPort(this, "pendingIn")
  /* Sources of tuples to be deleted in the next tick */
  val deleteIn = InputPort(this, "deletesIn")

  def hasInputs: Boolean = {
    !Seq(deltaIn, pendingIn, deleteIn).flatMap(_.connectedPorts).isEmpty
  }

  override def equals(other: Any): Boolean = other match {
    case that: Table => collection == that.collection
    case _ => false
  }

  override def hashCode(): Int = collection.hashCode()
}

case class MapElement(mapFunction: RowExpr)(implicit g: DataflowGraph, s: Stratum) extends DataflowElement {
  val input = InputPort(this, "input")
}

case class HashEquiJoinElement(buildKey: ColExpr, probeKey: ColExpr)(implicit g: DataflowGraph, s: Stratum) extends DataflowElement {
  val buildInput = InputPort(this, "buildInput")
  val probeInput = InputPort(this, "probeInput")
}

// TODO: might want to have more general "anti-join" and outer-join elements;
// This is an okay placeholder for now, since it lets me test the stratification.
case class NotInElement()(implicit g: DataflowGraph, s: Stratum) extends DataflowElement {
  val probeInput = InputPort(this, "probeInput")
  val tableInput = InputPort(this, "tableInput")
}