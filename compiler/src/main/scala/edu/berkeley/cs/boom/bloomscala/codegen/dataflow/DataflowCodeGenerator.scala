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

  private def addElementsForRule(graph: DataflowGraph, depAnalyzer: DepAnalyzer, stmt: Statement, stratum: Stratum) {
    implicit val g = graph
    implicit val s = stratum
    stmt match { case Statement(lhs, op, rhs, number) =>
      // Create the dataflow element that produces the RHS and return its OutputPort:
      val rhsOutput: OutputPort = rhs match {
        case cr: CollectionRef =>
          graph.collections(cr.collection).scanner.output
        case MappedCollection(cr: CollectionRef, tupVars, rowExpr) =>
          val mapElem = MapElement(rowExpr, 1)
          mapElem.input <-> graph.collections(cr.collection).scanner.output
          mapElem.output
        case JoinedCollections(List(a, b), List(EqualityPredicate(aExpr, bExpr)), tupVars, rowExpr) =>
          // TODO: generalize for 3+ tables.
          // We can implement this using a pair of stateful hash join operators,
          // one for each delta.
          val aSource = graph.collections(a.collection)
          val bSource = graph.collections(b.collection)
          val aDelta = new HashEquiJoinElement(aExpr, bExpr, true)
          val bDelta = new HashEquiJoinElement(aExpr, bExpr, false)
          aDelta.leftInput <-> aSource.scanner.output
          aDelta.rightInput <-> bSource.scanner.output
          bDelta.leftInput <-> aSource.scanner.output
          bDelta.rightInput <-> bSource.scanner.output
          val project = MapElement(rowExpr, 2)
          project.input <-> aDelta.output
          project.input <-> bDelta.output
          project.output
        case NotIn(a, b) =>
          val aSource = graph.collections(a.collection)
          val bSource = graph.collections(b.collection)
          val notin = new NotInElement()
          notin.probeInput <-> aSource.scanner.output
          notin.tableInput <-> bSource.scanner.output
          notin.output
        case ArgMin(collection, groupingCols, chooseExpr, func) =>
          val scanner = graph.collections(collection.collection).scanner
          val choose = ArgMinElement(groupingCols, chooseExpr, func)
          scanner.output <-> choose.input
          choose.output
      }
      // Wire the RHS's output to the LHS, with the connection type
      // dependent on the type of Bloom operator (<= vs <+ or <-):
      val lhsCollection = graph.collections(lhs.collection)
      import BloomOp._
      lhsCollection match {
        case table: Table => op match {
          case InstantaneousMerge =>
            rhsOutput <-> table.deltaIn
          case DeferredMerge =>
            rhsOutput <-> table.pendingIn
          case Delete =>
            rhsOutput <-> table.deleteIn
        }
      }
    }
  }

  final def generateCode(program: Program, stratifier: Stratifier, depAnalyzer: DepAnalyzer): CharSequence = {
    implicit val graph = new DataflowGraph(stratifier)
    program.statements.foreach(rule => addElementsForRule(graph, depAnalyzer, rule, stratifier.ruleStratum(rule)))
    generateCode(graph)
  }
}