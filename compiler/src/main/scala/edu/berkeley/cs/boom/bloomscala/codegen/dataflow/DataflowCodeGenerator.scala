package edu.berkeley.cs.boom.bloomscala.codegen.dataflow

import edu.berkeley.cs.boom.bloomscala.codegen.CodeGenerator
import edu.berkeley.cs.boom.bloomscala.ast._
import edu.berkeley.cs.boom.bloomscala.analysis.{Stratum, DepAnalyzer, Stratifier}

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

  private def addElementsForRule(graph: DataflowGraph, stmt: Statement, stratum: Stratum) {
    implicit val g = graph
    implicit val s = stratum
    stmt match { case Statement(lhs, op, rhs, number) =>
      // First, create the dataflow elements to produce the RHS.
      val rhsOutputs: Set[DataflowElement] = rhs match {
        case cr: CollectionRef =>
          Set(graph.tables(cr.collection))
        case MappedCollection(cr: CollectionRef, tupVars, rowExpr) =>
          val mapElem = MapElement(rowExpr, 1)
          mapElem.input <-> graph.tables(cr.collection).deltaOut
          Set(mapElem)
        case MappedEquijoin(a, b, aExpr, bExpr, tupVars, rowExpr) =>
          // We can implement this using a pair of stateful hash join operators,
          // one for each delta.
          val aTable = graph.tables(a.collection)
          val bTable = graph.tables(b.collection)
          val aDelta = new HashEquiJoinElement(aExpr, bExpr, true)
          val bDelta = new HashEquiJoinElement(aExpr, bExpr, false)
          aDelta.leftInput <-> aTable.deltaOut
          aDelta.rightInput <-> bTable.deltaOut
          bDelta.leftInput <-> aTable.deltaOut
          bDelta.rightInput <-> bTable.deltaOut
          val project = MapElement(rowExpr, 2)
          project.input <-> aDelta.deltaOut
          project.input <-> bDelta.deltaOut
          Set(project)
        case NotIn(a, b) =>
          val aTable = graph.tables(a.collection)
          val bTable = graph.tables(b.collection)
          val notin = new NotInElement()
          notin.probeInput <-> aTable.deltaOut
          notin.tableInput <-> bTable.deltaOut
          Set(notin)
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
    implicit val graph = new DataflowGraph(stratifier)
    program.statements.foreach(rule => addElementsForRule(graph, rule, stratifier.ruleStratum(rule)))
    generateCode(graph)
  }
}