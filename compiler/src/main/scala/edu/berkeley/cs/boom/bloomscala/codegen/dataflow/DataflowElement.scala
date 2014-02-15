package edu.berkeley.cs.boom.bloomscala.codegen.dataflow

import edu.berkeley.cs.boom.bloomscala.analysis.Stratum
import scala.collection.mutable

/**
 * Represents a generic push-based dataflow element.
 */
class DataflowElement(implicit graph: DataflowGraph, implicit val stratum: Stratum) {
  /** A unique identifier for this dataflow element */
  val id = graph.nextElementId.getAndIncrement

  val inputPorts = mutable.HashSet[InputPort]()
  val outputPorts = mutable.HashSet[OutputPort]()
  val connectedStems = mutable.HashSet[StateModule]()

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