package edu.berkeley.cs.boom.bloomscala

import edu.berkeley.cs.boom.bloomscala.ast.Program
import scala.collection.{GenSeq, GenMap}
import edu.berkeley.cs.boom.bloomscala.analysis.{Stratifier, DepAnalyzer, Stratum}


// TODO: this test currently has a ton of code duplication.
// This will be eliminated once the stratifcation results are embedded into rules
// via a rewriting phase rather than relying on attribution.

class StratifierSuite extends BloomScalaSuite {

  def isStratifiable(source: String) = {
    val program = Compiler.nameAndType(source)
    val depAnalyzer = new DepAnalyzer(program)
    val stratifier = new Stratifier(depAnalyzer)
    import stratifier._
    program->isTemporallyStratifiable
  }

  def getCollectionStrata(program: Program): GenMap[String, Stratum] = {
    val depAnalyzer = new DepAnalyzer(program)
    val stratifier = new Stratifier(depAnalyzer)
    import stratifier._
    program.declarations.map(d => (d.name, collectionStratum(d))).toMap
  }

  def getRuleStrata(program: Program): GenSeq[Stratum] = {
    val depAnalyzer = new DepAnalyzer(program)
    val stratifier = new Stratifier(depAnalyzer)
    import stratifier._
    program.statements.map(ruleStratum).toSeq
  }

  test("Positive programs should have only one stratum") {
    val program = Compiler.compileToIntermediateForm(
      """
        |      table link, [from: string, to: string, cost: int]
        |      table path, [from: string, to: string, nxt: string, cost: int]
        |      path <= link {|l| [l.from, l.to, l.to, l.cost]}
        |      path <= (link * path) on (link.to == path.from) { |l, p|
        |        [l.from, p.to, l.to, l.cost+p.cost]
        |      }
      """.stripMargin)
      val depAnalyzer = new DepAnalyzer(program)
      val stratifier = new Stratifier(depAnalyzer)
      import stratifier._
      assert(program->isTemporallyStratifiable)
      assert(getCollectionStrata(program).values.toSet.size === 1)
      assert(getRuleStrata(program).toSet.size === 1)
   }

  test("Collections should be placed in higher strata than their negated dependencies") {
    val program = Compiler.compileToIntermediateForm(
      """
        |     table a, [val: int]
        |     table b, [val: int]
        |     table c, [val: int]
        |     c <= a.notin(b)
      """.stripMargin
    )
    val depAnalyzer = new DepAnalyzer(program)
    val stratifier = new Stratifier(depAnalyzer)
    import stratifier._
    assert(program->isTemporallyStratifiable)
    val strata = getCollectionStrata(program)
    assert(strata("a") === strata("b"))
    assert(strata("c") > strata("b"))
    assert(getRuleStrata(program).head === strata("c"))
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

  test("Dependencies used in non-monotonic contexts are evaluated in lower strata") {
    val program = Compiler.compileToIntermediateForm(
      """
        |     table a, [key: int, val: int]
        |     table b, [ley: int, val: int]
        |     b <= a.argmin([a.key], a.val, intOrder)
      """.stripMargin
    )
    val depAnalyzer = new DepAnalyzer(program)
    val stratifier = new Stratifier(depAnalyzer)
    import stratifier._
    assert(program->isTemporallyStratifiable)
    val strata = getCollectionStrata(program)
    assert(strata("a") === Stratum(0))
    assert(strata("b") === Stratum(1))
    assert(getRuleStrata(program).head === Stratum(1))
  }
}
