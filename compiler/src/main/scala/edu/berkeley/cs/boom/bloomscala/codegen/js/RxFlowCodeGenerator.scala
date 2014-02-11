package edu.berkeley.cs.boom.bloomscala.codegen.js

import edu.berkeley.cs.boom.bloomscala.codegen.dataflow._
import edu.berkeley.cs.boom.bloomscala.codegen.dataflow.HashEquiJoinElement
import edu.berkeley.cs.boom.bloomscala.codegen.dataflow.Table

/**
 * Compiles Bloom programs to Javascript that use the RxJs and RxFlow libraries.
 *
 * The output contains a Javascript object that exposes Bloom collections as
 * objects that implement the Rx Observable and Observer interfaces.
 *
 * myObj.collectionNameIn is an Observer that can be used to push new tuples
 * into the system by calling myObj.collectionNameIn.onNext(tuple).
 *
 * myObj.collectionNameOut is a Subject that can be subscribed() to
 * in order to receive callbacks when new tuples are added to collections.
 */
object RxFlowCodeGenerator extends DataflowCodeGenerator with JsCodeGeneratorUtils {

  private def elemName(elem: DataflowElement): Doc = {
    elem match {
      case Table(collection) => text(collection.name)
      case e => text("elem" + e.id)
    }
  }

  private def portName(inputPort: InputPort): Doc = {
    inputPort.elem match {
      case HashEquiJoinElement(_, _, _) =>
        inputPort.name match {
          case "leftInput" => elemName(inputPort.elem) <> dot <> "leftInput"
          case "rightInput" => elemName(inputPort.elem) <> dot <> "rightInput"
        }
      case Table(collection) =>
        inputPort.name match {
          case "deltaIn" => elemName(inputPort.elem) <> "Delta"
        }
      case _ => elemName(inputPort.elem) <> dot <> "input"
    }
  }

  private def portName(outputPort: OutputPort): Doc = {
    outputPort.elem match {
      case Table(_) => elemName(outputPort.elem) <> "Delta"
      case _ => elemName(outputPort.elem) <> dot <> "output"
    }
  }

  override def generateCode(graph: DataflowGraph): CharSequence = {
    val tableNames = graph.tables.values.map(_.collection.name)

    val deltaSubjects = tableNames.map(name => text(s"var ${name}Delta = new Rx.Subject();"))

    val deltaIns = tableNames.map { name =>
      s"this.${name}In = new Rx.Subject();" <@@>
      s"this.${name}In.subscribe(${name}Delta);" <> linebreak
    }

    val deltaOuts = tableNames.map { name =>
      s"this.${name}Out = new Rx.Subject();" <@@>
      s"${name}Delta.distinct().subscribe(this.${name}Out);" <> linebreak
    }

    val dataflowRules = graph.stratifiedElements.flatMap { case (stratum, allElements) =>
      val elements = allElements.filterNot(_.isInstanceOf[Table]).toSeq
      val elementCreation = elements.map {
        case hj @ HashEquiJoinElement(buildKey, probeKey, leftIsBuild) =>
          "var" <+> elemName(hj) <+> equal <+> "new" <+>
            methodCall("rxflow", "HashJoin",
              genLambda(buildKey, List("x")),
              genLambda(probeKey, List("x")),
              if (leftIsBuild) "\"left\"" else "\"right\""
            ) <> semi
        case mapElem @ MapElement(mapFunction, functionArity) =>
          val exprParameterNames =
            if (functionArity == 1) List("x")
            else (0 to functionArity - 1).map(x => s"x[$x]").toList
          "var" <+> elemName(mapElem) <+> equal <+> "new" <+>
            methodCall("rxflow", "Map", genLambda(mapFunction, List("x"), Some(exprParameterNames))) <> semi
        case elem => elemName(elem)
      }
      val elementWiring = allElements.flatMap {
        elem => elem.outputPorts.flatMap { outputPort => outputPort.connectedPorts.map {
            inputPort => portName(outputPort) <> dot <> functionCall("subscribe", portName(inputPort)) <> semi
          }
        }
      }
      elementCreation ++ elementWiring
    }

    val code = "function Bloom ()" <+> braces(nest(
      linebreak <>
      deltaSubjects.reduce(_ <@@> _) <@@>
      linebreak <>
      deltaIns.reduce(_ <@@> _) <@@>
      linebreak <>
      deltaOuts.reduce(_ <@@> _) <@@>
      linebreak <>
      dataflowRules.reduce(_ <@@> _)
    ) <> linebreak)
    super.pretty(code)
  }

}
