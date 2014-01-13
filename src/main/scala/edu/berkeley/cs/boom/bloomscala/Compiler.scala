package edu.berkeley.cs.boom.bloomscala

import com.typesafe.scalalogging.slf4j.Logging
import org.kiama.util.Messaging
import edu.berkeley.cs.boom.bloomscala.parser.AST._
import org.kiama.attribution.Attributable
import edu.berkeley.cs.boom.bloomscala.parser.BudParser
import edu.berkeley.cs.boom.bloomscala.analysis._


object Compiler extends Logging {

  val messaging = new Messaging
  val namer = new Namer(messaging)
  val typer = new Typer(messaging, namer)
  val depAnalyzer = new DepAnalayzer(messaging, namer)
  val stratifier = new Stratifier(messaging, depAnalyzer)
  import namer._
  import typer._
  import stratifier._

  def compile(src: String): Program = {
    messaging.resetmessages()
    try {
      val parseResults = BudParser.parseProgram(src)
      // Force evaluation of the typechecking:
      // TODO: the definition of a program being well-typed
      // should be an attribute at the root of the tree that's
      // defined recursively over the program's statements
      // and subtrees
      def check(node: Attributable) {
        node match {
          case cr: CollectionRef => cr->collectionDeclaration
          case s: Statement => s->isWellTyped
          case _ =>
        }
        node.children.foreach(check)
      }
      check(parseResults)
      parseResults
    } catch { case e: Exception =>
      logger.error(s"Compilation failed: ${e.getMessage}")
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
      compile(p)
  }
}