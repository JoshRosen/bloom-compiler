package edu.berkeley.cs.boom.bloomscala

import scala.util.parsing.combinator._
import scala.util.parsing.input.Positional


case class CollectionDeclaration(collectionType: String, name: String, keys: List[String], values: List[String]) extends Positional

object BloomOp extends Enumeration {
  type BloomOp = Value
  val InstantaneousMerge, DeferredMerge, AsynchronousMerge, Delete, DeferredUpdate = Value
  val symbolToOp: Map[String, BloomOp] = Map(
    "<=" -> InstantaneousMerge,
    "<+" -> DeferredMerge,
    "<~" -> AsynchronousMerge,
    "<-" -> Delete,
    "<+-" -> DeferredUpdate
  )
}

trait BudParsers extends JavaTokenParsers {
  import edu.berkeley.cs.boom.bloomscala.BloomOp._
  // See: http://stackoverflow.com/questions/2382644
  override val whiteSpace = """[ \t]+""".r
  def eol: Parser[Any] = """(\r?\n)+""".r
  // TODO Reserved words

  def collectionType: Parser[String] = "table" | "scratch" | failure("Unknown collection type")
  def collectionDeclaration =
    positioned((collectionType ~ ident ~ "," ~ opt(columnsDeclaration ~ opt("=>" ~> columnsDeclaration)) <~ eol) ^^ {
      case collectionType ~ ident ~ "," ~ keyVals =>
        val keys = keyVals.map(_._1).getOrElse(List.empty)
        val values = keyVals.flatMap(_._2).getOrElse(List.empty)
        new CollectionDeclaration(collectionType, ident, keys, values)
    })
  def columnsDeclaration: Parser[List[String]] = "[" ~> rep1sep(tableColumn, ",") <~ "]"
  def tableColumn = ident

  def expression = ident
  def bloomOp = BloomOp.symbolToOp.keySet.map(literal).reduce(_ | _) ^^ BloomOp.symbolToOp.apply

  def statement = ident ~ bloomOp ~ expression <~ eol
  def program = rep(statement | collectionDeclaration | eol)

}


object BudParsersMain extends BudParsers {
  def main(args: Array[String]) {
    val p =
      """
      table clouds, [apple, bannanna] => [value, token]
      scratch temporary, [apple, bannanna]
      clouds <~ HELLO
      """.stripMargin
    val result = parseAll(program, p)
    println("The results are: " + result)
  }
}