package edu.berkeley.cs.boom.bloomscala.codegen.dataflow

import edu.berkeley.cs.boom.bloomscala.analysis.Stratum
import scala.collection.mutable
import edu.berkeley.cs.boom.bloomscala.ast.CollectionDeclaration

/**
 * Represents a generic push-based dataflow element.
 */
class DataflowElement(implicit graph: DataflowGraph, implicit val stratum: Stratum) {
  /** A unique identifier for this dataflow element */
  val id = graph.nextElementId.getAndIncrement

  val inputPorts = mutable.HashSet[InputPort]()
  val outputPorts = mutable.HashSet[OutputPort]()

  def upstreamElements = inputPorts.flatMap(ip => ip.connections.map(e => e.from.elem)).toSet
  def downstreamElements = outputPorts.flatMap(op => op.connections.map(e => e.to.elem)).toSet

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
 * Base class for dataflow elements that have separate scanner elements (e.g. Tables).
 */
class ScannableDataflowElement(val collection: CollectionDeclaration)
                              (implicit graph: DataflowGraph, stratum: Stratum) extends RescannableDataflowElement {
  val scanner = new Scanner(this)

  override def downstreamElements = Set(scanner)

  /**
   * Return the index of the last key column.
   *
   * Assumes that records are of the form [keyCol1, keyCol2, ... , valCol1, valCol2, ...].
   * If lastKeyColIndex == len(record) - 1, then the entire record is treated as the key
   * and the table functions like a set.
   */
  def lastKeyColIndex: Int = {
    collection.keys.length - 1
  }
}

/**
 * Class for dataflow elements that maintain internal state that must be invalidated if
 * any of their inputs perform rescans.
 */
class StatefulDataflowElement(implicit graph: DataflowGraph, stratum: Stratum) extends DataflowElement

/**
 * Mixin trait for stateful dataflow elements that can perform
 * rescans out of their caches rather than having to rescan
 * their inputs.
 */
class RescannableDataflowElement(implicit graph: DataflowGraph, stratum: Stratum) extends StatefulDataflowElement
