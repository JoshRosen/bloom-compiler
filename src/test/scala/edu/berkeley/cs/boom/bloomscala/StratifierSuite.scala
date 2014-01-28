package edu.berkeley.cs.boom.bloomscala

import org.scalatest.FunSuite
import com.typesafe.scalalogging.slf4j.Logging
import edu.berkeley.cs.boom.bloomscala.ast.Program
import scala.collection.{GenSeq, GenMap}
import edu.berkeley.cs.boom.bloomscala.analysis.{Stratifier, DepAnalyzer, Stratum}


// TODO: this test currently has a ton of code duplication.
// This will be eliminated once the stratifcation results are embedded into rules
// via a rewriting phase rather than relying on attribution.

class StratifierSuite extends FunSuite with Logging {

  def isStratifiable(source: String) = {
    val program = Compiler.nameAndType(source)
    val depAnalyzer = new DepAnalyzer(program)
    val stratifier = new Stratifier(Compiler.messaging, depAnalyzer)
    import stratifier._
    program->isTemporallyStratifiable
  }

  def getCollectionStrata(program: Program): GenMap[String, Stratum] = {
    val depAnalyzer = new DepAnalyzer(program)
    val stratifier = new Stratifier(Compiler.messaging, depAnalyzer)
    import stratifier._
    program.declarations.map(d => (d.name, collectionStratum(d))).toMap
  }

  def getRuleStrata(program: Program): GenSeq[Stratum] = {
    val depAnalyzer = new DepAnalyzer(program)
    val stratifier = new Stratifier(Compiler.messaging, depAnalyzer)
    import stratifier._
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
      val depAnalyzer = new DepAnalyzer(program)
      val stratifier = new Stratifier(Compiler.messaging, depAnalyzer)
      import stratifier._
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
    val depAnalyzer = new DepAnalyzer(program)
    val stratifier = new Stratifier(Compiler.messaging, depAnalyzer)
    import stratifier._
    assert(program->isTemporallyStratifiable)
    val strata = getCollectionStrata(program)
    assert(strata("a") === Stratum(0))
    assert(strata("b") === Stratum(0))
    assert(strata("c") === Stratum(1))
    assert(getRuleStrata(program).head === Stratum(1))
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
