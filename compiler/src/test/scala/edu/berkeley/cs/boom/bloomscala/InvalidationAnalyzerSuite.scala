package edu.berkeley.cs.boom.bloomscala

import org.scalatest.FunSuite
import edu.berkeley.cs.boom.bloomscala.codegen.dataflow.InvalidationAnalyzer._
import edu.berkeley.cs.boom.bloomscala.analysis.Stratum
import edu.berkeley.cs.boom.bloomscala.codegen.dataflow._
import edu.berkeley.cs.boom.bloomscala.codegen.dataflow.Scanner
import edu.berkeley.cs.boom.bloomscala.codegen.dataflow.MapElement

class InvalidationAnalyzerSuite extends FunSuite {
  test("invalidation of one of an element's inputs triggers rescan of all of its inputs") {
    implicit val graph = new DataflowGraph(null)
    implicit val stratum = Stratum(0)

    val scannerA = Scanner(null)
    val mapA = MapElement(null, 1)
    scannerA.output <-> mapA.input

    val scannerB = Scanner(null)
    val mapB = MapElement(null, 1)
    scannerB.output <-> mapB.input

    val join = SymmetricHashEquiJoinElement(null, null)
    mapA.output <-> join.leftInput
    mapB.output <-> join.rightInput

    assert(rescanSet(Set(scannerA)) === Set(scannerA, scannerB))
    assert(invalidateSet(Set(scannerA)).contains(join))

    assert(rescanSet(Set(scannerB)) === Set(scannerA, scannerB))
    assert(invalidateSet(Set(scannerB)).contains(join))

    assert(rescanSet(Set(scannerA, scannerB)) === Set(scannerA, scannerB))
    assert(invalidateSet(Set(scannerA, scannerB)).contains(join))
  }

  test("rescanable elements aren't invalid unless they have invalid inputs") {
    implicit val graph = new DataflowGraph(null)
    implicit val stratum = Stratum(0)

    val scannerA = Scanner(null)
    val scannerB = Scanner(null)
    val scannerC = Scanner(null)

    val joinAB = SymmetricHashEquiJoinElement(null, null)
    scannerA.output <-> joinAB.leftInput
    scannerB.output <-> joinAB.rightInput

    val joinABC = SymmetricHashEquiJoinElement(null, null)
    joinAB.output <-> joinABC.leftInput
    scannerC.output <-> joinABC.rightInput

    assert(rescanSet(Set(scannerC)) === Set(scannerC, joinAB))
    assert(invalidateSet(Set(scannerC)).contains(joinABC))
    assert(!invalidateSet(Set(scannerC)).contains(joinAB))
  }

  test("stateful elements aren't always rescanable") {
    implicit val graph = new DataflowGraph(null)
    implicit val stratum = Stratum(0)

    val scannerA = Scanner(null)
    val scannerB = Scanner(null)
    val scannerC = Scanner(null)

    val joinAB = HashEquiJoinElement(null, null, true)
    scannerA.output <-> joinAB.leftInput
    scannerB.output <-> joinAB.rightInput

    val joinABC = SymmetricHashEquiJoinElement(null, null)
    joinAB.output <-> joinABC.leftInput
    scannerC.output <-> joinABC.rightInput

    assert(rescanSet(Set(scannerC)) === Set(scannerA, scannerB, scannerC))
    assert(invalidateSet(Set(scannerC)).contains(joinABC))
    assert(invalidateSet(Set(scannerC)).contains(joinAB))
  }
}
