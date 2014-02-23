package edu.berkeley.cs.boom.bloomscala.codegen.dataflow

import edu.berkeley.cs.boom.bloomscala.ast._
import edu.berkeley.cs.boom.bloomscala.analysis.Stratum


case class Table(override val collection: CollectionDeclaration)
                (implicit g: DataflowGraph, s: Stratum) extends ScannableDataflowElement(collection) {
  /** Sources of new tuples to process in the current tick */
  val deltaIn = InputPort(this, "deltaIn")
  /* Sources of tuples to be added in the next tick */
  val pendingIn = InputPort(this, "pendingIn")
  /* Sources of tuples to be deleted in the next tick */
  val deleteIn = InputPort(this, "deletesIn")

  def hasInputs: Boolean = {
    !Seq(deltaIn, pendingIn, deleteIn).flatMap(_.connections).isEmpty
  }
}

case class InputElement(override val collection: CollectionDeclaration)
                       (implicit g: DataflowGraph, s: Stratum) extends ScannableDataflowElement(collection) {
  val output = OutputPort(this, "output")
}

case class OutputElement(val collection: CollectionDeclaration)
                        (implicit g: DataflowGraph, s: Stratum) extends DataflowElement {
  val input = InputPort(this, "input")
}

case class Scanner(table: ScannableDataflowElement)(implicit g: DataflowGraph, s: Stratum) extends DataflowElement {
  val output = OutputPort(this, "output")
}

case class MapElement(mapFunction: RowExpr, functionArity: Int)(implicit g: DataflowGraph, s: Stratum) extends DataflowElement {
  val input = InputPort(this, "input")
  val output = OutputPort(this, "output")
}

case class ArgMinElement(groupingCols: List[FieldRef], chooseExpr: Expr, function: FunctionRef)
                        (implicit g: DataflowGraph, s: Stratum) extends DataflowElement with Rescanable {
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