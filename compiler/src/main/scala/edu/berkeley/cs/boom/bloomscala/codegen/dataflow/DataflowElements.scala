package edu.berkeley.cs.boom.bloomscala.codegen.dataflow

import scala.collection.mutable
import edu.berkeley.cs.boom.bloomscala.ast._
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

/**
 * Represents a generic push-based dataflow element.
 */
class DataflowElement(implicit graph: DataflowGraph, implicit val stratum: Stratum) {
  /** A unique identifier for this dataflow element */
  val id = graph.nextElementId.getAndIncrement

  val inputPorts = mutable.HashSet[InputPort]()
  val outputPorts = mutable.HashSet[OutputPort]()

  def upstreamElements = inputPorts.flatMap(ip => ip.connectedPorts.map(op => op.elem)).toSet
  def downstreamElements = outputPorts.flatMap(op => op.connectedPorts.map(ip => ip.elem)).toSet

  // This statement needs to be AFTER we assign the id so that hashCode()
  // and equals() return the right results when we add this element to the
  // hashSet:
  graph.elements += this

  override def equals(other: Any): Boolean = other match {
    case that: DataflowElement => id == that.id
    case _ => false
  }                 

  override def hashCode(): Int = id
}

/**
 * Mixin trait for dataflow elements that maintain internal state
 * that must be invalidated if any of their inputs perform rescans.
 */
trait Stateful

/**
 * Mixin trait for stateful dataflow elements that can perform
 * rescans out of their caches rather than having to rescan
 * their inputs.
 */
trait Rescanable extends Stateful

case class Table(collection: CollectionDeclaration)(implicit g: DataflowGraph, s: Stratum) extends DataflowElement {
  /** Sources of new tuples to process in the current tick */
  val deltaIn = InputPort(this, "deltaIn")
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

  val scanner = new Scanner(this)
}

case class Scanner(table: Table)(implicit g: DataflowGraph, s: Stratum) extends DataflowElement {
  val output = OutputPort(this, "output")
}

case class MapElement(mapFunction: RowExpr, functionArity: Int)(implicit g: DataflowGraph, s: Stratum) extends DataflowElement {
  val input = InputPort(this, "input")
  val output = OutputPort(this, "output")
}

case class HashEquiJoinElement(leftKey: ColExpr, rightKey: ColExpr, leftIsBuild: Boolean)
                              (implicit g: DataflowGraph, s: Stratum) extends DataflowElement with Stateful {
  val leftInput = InputPort(this, "leftInput")
  val rightInput = InputPort(this, "rightInput")
  val output = OutputPort(this, "output")
}

case class SymmetricHashEquiJoinElement(leftKey: ColExpr, rightKey: ColExpr)
                                       (implicit g: DataflowGraph, s: Stratum) extends DataflowElement with Rescanable {
  val leftInput = InputPort(this, "leftInput")
  val rightInput = InputPort(this, "rightInput")
  val output = OutputPort(this, "output")
}

// TODO: might want to have more general "anti-join" and outer-join elements;
// This is an okay placeholder for now, since it lets me test the stratification.
case class NotInElement()(implicit g: DataflowGraph, s: Stratum) extends DataflowElement {
  val probeInput = InputPort(this, "probeInput")
  val tableInput = InputPort(this, "tableInput")
  val output = OutputPort(this, "output")
}