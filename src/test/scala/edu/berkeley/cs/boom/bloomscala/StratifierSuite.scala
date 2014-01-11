package edu.berkeley.cs.boom.bloomscala

import org.scalatest.FunSuite
import edu.berkeley.cs.boom.bloomscala.parser.BudParser
import edu.berkeley.cs.boom.bloomscala.Stratifier.StratifiedCollections
import com.typesafe.scalalogging.slf4j.Logging


class StratifierSuite extends FunSuite with Logging {

  def stratify(program: String): StratifiedCollections = {
    val parseResults = BudParser.parseProgram(program)
    implicit val info = new AnalysisInfo(parseResults)
    new Typer().run()
    try {
      val stratifier = new Stratifier()
      stratifier.buildPrecedenceGraph()
      val strata = stratifier.stratifyCollections()
      stratifier.stratifyRules(strata)
      strata
    } catch { case e: Exception =>
      logger.error(e.getMessage)
      throw e
    }
  }

  test("Positive programs should have only one stratum") {
    val strata = stratify(
      """
        |      table link, [from: string, to: string, cost: int]
        |      table path, [from: string, to: string, nxt: string, cost: int]
        |      path <= link {|l| [l.from, l.to, l.to, l.cost]}
        |      path <= (link * path) on (link.to == path.from) { |l, p|
        |        [l.from, p.to, l.to, l.cost+p.cost]
        |      }
      """.stripMargin)
      assert(strata.length === 1)
    }

  test("Cycles with temporal negation should still be stratifiable") {
    stratify(
      """
        |       table a, [val: int]
        |       table b, [val: int]
        |       a <+ b.notin(a)
        |       b <+ a.notin(b)
      """.stripMargin)
  }

  test("Cycles with immediate negation should be unstratifiable") {
    intercept[StratificationError] {
      stratify(
        """
          |       table a, [val: int]
          |       table b, [val: int]
          |       b <= a.notin(b)
          |       a <= b.notin(a)
        """.stripMargin)
     }
  }
}
