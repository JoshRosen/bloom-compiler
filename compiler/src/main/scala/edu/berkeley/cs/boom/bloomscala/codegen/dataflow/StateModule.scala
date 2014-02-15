package edu.berkeley.cs.boom.bloomscala.codegen.dataflow

import edu.berkeley.cs.boom.bloomscala.analysis.Stratum
import edu.berkeley.cs.boom.bloomscala.ast.{CollectionDeclaration, Predicate}
import scala.collection.mutable

/**
 * Stateful dataflow element (SteM) that stores a set of tuples and supports insert,
 * search, and delete operations.  SteMs can be used to encapsulate indexes on data
 * sources and allow those indexes to be shared by multiple joins.
 *
 * This is based on SteMs as described in the paper
 *
 *     Raman, Vijayshankar, Amol Deshpande, and Joseph M. Hellerstein.
 *     "Using state modules for adaptive query processing."
 *     ICDE 2003. (http://db.cs.berkeley.edu/papers/icde03-stems.pdf).
 *
 *  In the SteM abstraction, a single StEM supports lookups based on arbitrary predicates.
 * There are a few reasons to prefer this over a design that creates one SteM per indexed
 * attribute:
 *
 *  - The actual dataflow is the same whether or not SteM lookups use indexes.
 *  - We may need to inspect the entire set of queries in order to choose which indexes
 *    to build.  The StateModule instance can serve as a rendezvous point for accumulating
 *    information on the set of join predicates.
 *
 * The actual dataflow implementation might choose to use separate objects for encapsulating
 * each index.
 */
case class StateModule(collection: CollectionDeclaration)(implicit graph: DataflowGraph, stratum: Stratum)
  extends DataflowElement with Rescanable {

  val buildInput = InputPort(this, "build")
  val deleteInput = InputPort(this, "delete")

  val connectedElements = mutable.HashSet[(DataflowElement, Predicate)]()

  /**
   * Wire a dataflow element to this SteM and perform lookups based on
   * a predicate.
   */
  def <->(elem: DataflowElement, predicate: Predicate) {
    elem.connectedStems += this
    connectedElements += ((elem, predicate))
  }

}