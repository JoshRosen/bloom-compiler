package edu.berkeley.cs.boom.bloomscala.codegen.dataflow

import edu.berkeley.cs.boom.bloomscala.codegen.CodeGenerator
import edu.berkeley.cs.boom.bloomscala.parser.AST._
import edu.berkeley.cs.boom.bloomscala.analysis.{DepAnalyzer, Stratifier}

/**
 * Base class for code generators targeting push-based dataflow systems.
 *
 * Translates programs into a generic dataflow intermediate language and provides
 * hooks that subclasses can use to instantiate that dataflow on a particular
 * runtime.
 */
trait DataflowCodeGenerator extends CodeGenerator {

  // TODO: implement checks for unconnected ports in the generated dataflow?
  // Some ports of Table may be unconnected if the program doesn't contain deletions,
  // for example, but other operators, like HashJoin, must have all of their inputs
  // connected.

  /**
   * Translate a dataflow graph to platform-specific code.
   */
  def generateCode(dataflowGraph: DataflowGraph): CharSequence

  private def addElementsForRule(graph: DataflowGraph, stmt: Statement) {
    implicit val g = graph
    stmt match { case Statement(lhs, op, rhs, number) =>
      // First, create the dataflow elements to produce the RHS.
      val rhsOutputs: Set[DataflowElement] = rhs match {
        case cr: CollectionRef =>
          Set(graph.tables(cr.collection))
        case MappedCollection(cr: CollectionRef, tupVars, colExprs) =>
          val mapElem = MapElement(colExprs)
          mapElem.input <-> graph.tables(cr.collection).deltaOut
          Set(mapElem)
        case MappedEquijoin(a, b, aExpr, bExpr, tupVars, colExprs) =>
          // We can implement this using a pair of stateful hash join operators,
          // one for each delta.
          val aTable = graph.tables(a.collection)
          val bTable = graph.tables(b.collection)
          val aDelta = new HashEquiJoinElement(bExpr, aExpr)
          val bDelta = new HashEquiJoinElement(bExpr, aExpr)
          aDelta.buildInput <-> bTable.deltaOut
          aDelta.probeInput <-> aTable.deltaOut
          bDelta.buildInput <-> aTable.deltaOut
          bDelta.probeInput <-> bTable.deltaOut
          Set(aDelta, bDelta)
      }
      // Wire the final element's outputs to their targets, with the connection type
      // dependent on the type of Bloom operator (<= vs <+ or <-):
      val lhsTable: Table = graph.tables(lhs.collection)
      import BloomOp._
      op match {
        case InstantaneousMerge =>
          rhsOutputs.foreach { elem => elem.deltaOut <-> lhsTable.deltaIn}
        case DeferredMerge =>
          rhsOutputs.foreach { elem => elem.deltaOut <-> lhsTable.pendingIn}
        case Delete =>
          rhsOutputs.foreach { elem => elem.deleteOut <-> lhsTable.deleteIn}
      }

    }
  }

  final def generateCode(program: Program, stratifier: Stratifier, depAnalyzer: DepAnalyzer): CharSequence = {
    import depAnalyzer._
    import stratifier._

    // Wire up the dataflow graph:

    implicit val graph = new DataflowGraph()
    program.statements.foreach(rule => addElementsForRule(graph, rule))

    // Group the dataflow elements according to the stratum in which they should be placed
    // and pass the resulting graph to the next stage for code generation:

    val stratifiedCollections = program.declarations.groupBy(collectionStratum).toSeq.sortBy(x => x._1)
    generateCode(graph)
  }
}