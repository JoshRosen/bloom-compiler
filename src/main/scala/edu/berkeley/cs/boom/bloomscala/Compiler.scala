package edu.berkeley.cs.boom.bloomscala

import com.typesafe.scalalogging.slf4j.Logging
import org.kiama.util.Messaging
import edu.berkeley.cs.boom.bloomscala.parser.AST._
import edu.berkeley.cs.boom.bloomscala.parser.BudParser
import edu.berkeley.cs.boom.bloomscala.analysis._
import edu.berkeley.cs.boom.bloomscala.codegen.js.RxJsCodeGenerator


object Compiler extends Logging {

  val messaging = new Messaging
  private val namer = new Namer(messaging)
  private val typer = new Typer(messaging)

  def nameAndType(src: String): Program = {
    messaging.resetmessages()
    try {
      val parseResults = BudParser.parseProgram(src)
      val named = namer.resolveNames(parseResults)
      named.statements.map(typer.isWellTyped)
      named
    } catch { case e: Exception =>
      logger.error("Compilation failed", e)
      throw e
    } finally {
      messaging.report()
      if (messaging.messagecount != 0) {
        // TODO: this is fine for now for simple tests, but in the future
        // `compile` should return more detailed information for consumption
        // by unit tests
        throw new CompilerException("Compilation had error messages")
      }
    }
  }

  def compile(src: String): Program = {
    val typed = nameAndType(src)
    val depAnalyzer = new DepAnalyzer(typed)
    val stratifier = new Stratifier(messaging, depAnalyzer)
    stratifier.isTemporallyStratifiable(typed)
    typed
  }

  def main(args: Array[String]) {
    val p =
      """
      table link, [from: string, to: string, cost: int]
      table path, [from: string, to: string, nxt: string, cost: int]
      // Recursive rules to define all paths from links
      // Base case: every link is a path
      path <= link {|l| [l.from, l.to, l.to, l.cost]}
      // Inductive case: make a path of length n+1 by connecting a link to a
      // path of length n
      path <= (link * path) on (link.to == path.from) { |l, p|
        [l.from, p.to, l.to, l.cost+p.cost]
      }
      """.stripMargin
    val program = compile(p)
    val depAnalyzer = new DepAnalyzer(program)
    val stratifier = new Stratifier(messaging, depAnalyzer)
    val code = RxJsCodeGenerator.generateCode(program, stratifier, depAnalyzer)
    println(code)
  }
}