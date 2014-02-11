package edu.berkeley.cs.boom.bloomscala.codegen.dataflow

import org.kiama.attribution.Attribution._

/**
 * Statically analyzes the dataflow graph to determine when stateful dataflow elements'
 * cached states should be reset and when base relations need to be completely rescanned
 * (as opposed to incremental evaluation).
 *
 * Bud's rescan / invalidation logic is described at
 * https://github.com/bloom-lang/bud/blob/master/lib/bud/executor/README.rescan
 */
object InvalidationAnalyzer {

  /**
   * The set of elements that should invalidated if the given scanners are invalidated.
   */
  def invalidateSet(scanners: Set[Scanner]): Set[DataflowElement] = {
    if (scanners.isEmpty) return Set.empty[DataflowElement]
    // All downstream elements are invalidated:
    val downstream = scanners.flatMap(allDownstreamElements)
    // If these invalidations required additional rescans to be performed, we should
    // also include invalidations triggered by those extra rescans:
    val rescanningDeps = downstream.flatMap(rescanningDependencies(scanners))
    val rescanningScanners = rescanningDeps.filter(_.isInstanceOf[Scanner]).map(_.asInstanceOf[Scanner])
    val extraRescans = rescanningScanners -- scanners
    if (extraRescans.isEmpty)
      downstream
    else
      downstream ++ invalidateSet(extraRescans ++ scanners)
  }

  /**
   * The set of scanners and valid stateful elements that should perform
   * rescans if the given scanners are invalidated.
   */
  def rescanSet(scanners: Set[Scanner]): Set[DataflowElement] = {
    invalidateSet(scanners).flatMap(rescanningDependencies(scanners))
  }

  /**
   * Computes the set of elements that are transitively downstream of the given element.
   */
  lazy val allDownstreamElements: DataflowElement => Set[DataflowElement] =
    circular(Set.empty[DataflowElement]) {
      case elem: DataflowElement =>
        val immediateDownstream = elem.downstreamElements
        immediateDownstream ++ immediateDownstream.flatMap(allDownstreamElements)
    }

  /**
   * Computes the set of scanners that are transitively upstream of the given element.
   */
  lazy val upstreamScanners: DataflowElement => Set[Scanner] =
    attr {
      case scanner: Scanner => Set(scanner)
      case elem: DataflowElement => elem.upstreamElements.flatMap(upstreamScanners)
    }

  /**
   * Helper function that computes the set of scanners and valid stateful elements
   * that must perform rescans in order to recompute the given invalid element.
   *
   * This accounts for stateful operators that can satisfy rescans from their caches
   * as long as their inputs are still valid.
   *
   * @param invalidScanners the set of scanners currently known to be invalid
   * @param elem an invalid element whose inputs we are trying to rescan
   * @return the set of scanners that must rescan to recompute this element
   */
  private def rescanningDependencies(invalidScanners: Set[Scanner])(elem: DataflowElement): Set[DataflowElement] = {
    elem match {
      case scanner: Scanner => Set(scanner)
      case elem =>
        if (elem.isInstanceOf[Rescanable] && upstreamScanners(elem).intersect(invalidScanners).isEmpty)
          Set(elem)  // The stateful element's cache is still valid, so we'll serve rescans from it
        else
          elem.upstreamElements.flatMap(rescanningDependencies(invalidScanners))
    }
  }
}
