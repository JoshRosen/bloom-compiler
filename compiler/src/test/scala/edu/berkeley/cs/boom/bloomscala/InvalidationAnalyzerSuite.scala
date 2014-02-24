package edu.berkeley.cs.boom.bloomscala

import org.scalatest.{Matchers, FunSuite}
import edu.berkeley.cs.boom.bloomscala.codegen.dataflow.InvalidationAnalyzer._
import edu.berkeley.cs.boom.bloomscala.analysis.Stratum
import edu.berkeley.cs.boom.bloomscala.codegen.dataflow._
import edu.berkeley.cs.boom.bloomscala.codegen.dataflow.Scanner
import edu.berkeley.cs.boom.bloomscala.codegen.dataflow.MapElement

class InvalidationAnalyzerSuite extends FunSuite with Matchers {
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

    assert(invalidateSet(Set(scannerA)) === Set(scannerA, join))
    assert(rescanSet(Set(scannerA)) === Set(scannerA, scannerB))
    invalidateSet(Set(scannerA)) should contain (join)

    assert(rescanSet(Set(scannerB)) === Set(scannerA, scannerB))
    invalidateSet(Set(scannerB)) should contain (join)

    assert(rescanSet(Set(scannerA, scannerB)) === Set(scannerA, scannerB))
    invalidateSet(Set(scannerA, scannerB)) should contain (join)
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
    invalidateSet(Set(scannerC)) should contain (joinABC)
    invalidateSet(Set(scannerC)) should not contain (joinAB)
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
    invalidateSet(Set(scannerC)) should contain (joinABC)
    invalidateSet(Set(scannerC)) should contain (joinAB)
  }
}
