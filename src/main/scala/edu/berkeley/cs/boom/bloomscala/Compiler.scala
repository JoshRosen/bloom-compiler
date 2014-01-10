package edu.berkeley.cs.boom.bloomscala

import edu.berkeley.cs.boom.bloomscala.parser.BudParser


object Compiler {
  def compile(src: String) {
    val parseResults = BudParser.parseProgram(src)
    val info = new AnalysisInfo(parseResults)
    new Typer(info).run()
    new RuleGraphBuilder(info).run()
  }

  def main(args: Array[String]) {
    val p =
      """
      table link, [from: string, to: string, cost: int]
      table path, [from: string, to: string, nxt: string, cost: int]
      table shortest, [from: string, to: string] => [nxt: string, cost: string]
      // Recursive rules to define all paths from links
      // Base case: every link is a path
      path <= link {|l| [l.from, l.to, l.to, l.cost]}
      // Inductive case: make a path of length n+1 by connecting a link to a
      // path of length n
      path <= (link * path) on (link.to == path.from) { |l, p|
        [l.from, p.to, l.to, l.cost+p.cost]
      }
      """.stripMargin
    try {
      compile(p)
    } catch { case e: Exception =>
      println(e.getMessage)
      sys.exit(-1)
    }
  }
}