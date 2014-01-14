package edu.berkeley.cs.boom.bloomscala

import org.scalatest.FunSuite
import com.typesafe.scalalogging.slf4j.Logging
import edu.berkeley.cs.boom.bloomscala.parser.AST.Program
import scala.collection.{GenSeq, GenMap}


class StratifierSuite extends FunSuite with Logging {

  import Compiler.stratifier._

  def isStratifiable(source: String) = {
    val program = Compiler.compile(source)
    program->isTemporallyStratifiable
  }

  def getCollectionStrata(program: Program): GenMap[String, Int] = {
    program.declarations.map(d => (d.name, collectionStratum(d))).toMap
  }

  def getRuleStrata(program: Program): GenSeq[Int] = {
    program.statements.map(ruleStratum).toSeq
  }

  test("Positive programs should have only one stratum") {
    val program = Compiler.compile(
      """
        |      table link, [from: string, to: string, cost: int]
        |      table path, [from: string, to: string, nxt: string, cost: int]
        |      path <= link {|l| [l.from, l.to, l.to, l.cost]}
        |      path <= (link * path) on (link.to == path.from) { |l, p|
        |        [l.from, p.to, l.to, l.cost+p.cost]
        |      }
      """.stripMargin)
      assert(program->isTemporallyStratifiable)
      assert(getCollectionStrata(program).values.toSet.size === 1)
      assert(getRuleStrata(program).toSet.size === 1)
   }

  test("Collections should be placed in higher strata than their negated dependencies") {
    val program = Compiler.compile(
      """
        |     table a, [val: int]
        |     table b, [val: int]
        |     table c, [val: int]
        |     c <= a.notin(b)
      """.stripMargin
    )
    assert(program->isTemporallyStratifiable)
    val strata = getCollectionStrata(program)
    assert(strata("a") === 0)
    assert(strata("b") === 0)
    assert(strata("c") === 1)
    assert(getRuleStrata(program).head === 1)
  }

  test("Cycles with temporal negation should still be stratifiable") {
    assert(isStratifiable(
      """
        |       table a, [val: int]
        |       table b, [val: int]
        |       a <+ b.notin(a)
        |       b <+ a.notin(b)
      """.stripMargin))
  }

  test("Cycles with immediate negation should be unstratifiable") {
    assert(!isStratifiable(
        """
          |       table a, [val: int]
          |       table b, [val: int]
          |       b <= a.notin(b)
          |       a <= b.notin(a)
        """.stripMargin))
  }

  test("Positive cycles should be stratifiable") {
    assert(isStratifiable(
      """
        |       table a, [val: int]
        |       table b, [val: int]
        |       b <= a
        |       a <= b
      """.stripMargin))
  }
}
