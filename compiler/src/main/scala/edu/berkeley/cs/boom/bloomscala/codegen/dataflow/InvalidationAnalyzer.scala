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
   * The set of elements that should invalidated if the given stateful elements are invalidated.
   */
  def invalidateSet(elements: Set[StatefulDataflowElement]): Set[StatefulDataflowElement] = {
    if (elements.isEmpty) return Set.empty[StatefulDataflowElement]
    // All downstream elements are invalidated:
    val downstream = elements.flatMap(allDownstreamStatefulElements)
    // If these invalidations required additional rescans to be performed, we should
    // also include invalidations triggered by those extra rescans:
    val rescanningDeps = downstream.flatMap(rescanningDependencies(elements))
    val rescanningElems =
      rescanningDeps.filter(_.isInstanceOf[StatefulDataflowElement]).map(_.asInstanceOf[StatefulDataflowElement])
    val extraRescans = rescanningElems -- elements
    if (extraRescans.isEmpty) {
      elements ++ downstream
    } else {
      downstream ++ invalidateSet(extraRescans ++ elements) -- extraRescans.filter(canRescanFromCache(elements))
    }
  }

  /**
   * The set of valid stateful elements that should perform rescans if the given elements are invalidated.
   */
  def rescanSet(elements: Set[StatefulDataflowElement]): Set[StatefulDataflowElement] = {
    invalidateSet(elements).flatMap(rescanningDependencies(elements))
  }

  /**
   * Computes the set of stateful elements that are transitively downstream of the given element.
   */
  private lazy val allDownstreamStatefulElements: DataflowElement => Set[StatefulDataflowElement] =
    circular(Set.empty[StatefulDataflowElement]) {
      case elem: DataflowElement =>
        val result = elem.downstreamElements ++ elem.downstreamElements.flatMap(allDownstreamStatefulElements)
        result.filter(_.isInstanceOf[StatefulDataflowElement]).map(_.asInstanceOf[StatefulDataflowElement])
    }

  /**
   * Computes the set of stateful elements that are transitively upstream of the given element.
   */
  private lazy val allUpstreamStatefulElements: DataflowElement => Set[StatefulDataflowElement] =
    circular(Set.empty[StatefulDataflowElement]) {
      case elem: DataflowElement =>
        val result = elem.upstreamElements ++ elem.upstreamElements.flatMap(allUpstreamStatefulElements)
        result.filter(_.isInstanceOf[StatefulDataflowElement]).map(_.asInstanceOf[StatefulDataflowElement])
    }

  private def canRescanFromCache(invalidElements: Set[StatefulDataflowElement])
                                (elem: DataflowElement): Boolean = {
    elem match {
      case rescannable: RescannableDataflowElement =>
        allUpstreamStatefulElements(elem).intersect(invalidElements.toSet).isEmpty
      case _ =>
        false
    }
  }

  /**
   * Helper function that computes the set of valid stateful elements
   * that must perform rescans in order to recompute the given element.
   *
   * This accounts for stateful operators that can satisfy rescans from their caches
   * as long as their inputs are still valid.
   *
   * @param invalidElements the set of elements currently known to be invalid
   * @param elem an element that we're trying to rescan
   * @return the set of elements that must rescan in order to rescan this element
   */
  private def rescanningDependencies(invalidElements: Set[StatefulDataflowElement])
                                    (elem: DataflowElement): Set[RescannableDataflowElement] = {
    if (canRescanFromCache(invalidElements)(elem)) {
      Set(elem.asInstanceOf[RescannableDataflowElement])
    } else {
      elem.upstreamElements.flatMap(rescanningDependencies(invalidElements))
    }
  }
}
