package edu.berkeley.cs.boom.bloomscala

import scala.util.parsing.combinator._
import scala.util.parsing.input.Positional
import edu.berkeley.cs.boom.bloomscala.BloomCollectionType.BloomCollectionType


case class CollectionDeclaration(
    collectionType: BloomCollectionType,
    name: String,
    keys: List[String],
    values: List[String])
  extends Positional

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

object BloomCollectionType extends Enumeration {
  type BloomCollectionType = Value
  val Table, Scratch = Value
  val nameToType: Map[String, BloomCollectionType] = Map(
    "table" -> Table,
    "scratch" -> Scratch
  )
}

trait BudParsers extends JavaTokenParsers {
  // See: http://stackoverflow.com/questions/2382644
  override val whiteSpace = """[ \t]+""".r
  def eol: Parser[Any] = """(\r?\n)+""".r
  // TODO Reserved words

  def alternatives[T](map: Map[String, T], failMsg: String) =
    (map.keySet.map(literal).reduce(_ | _) | failure(failMsg)) ^^ map.apply

  def collectionType = alternatives(BloomCollectionType.nameToType, "Invalid collection type")
  def bloomOp = alternatives(BloomOp.symbolToOp, "Invalid operator")

  /********** Declarations **********/

  def collectionDeclaration =
    positioned((collectionType ~ ident ~ "," ~ opt(columnsDeclaration ~ opt("=>" ~> columnsDeclaration)) <~ eol) ^^ {
      case collectionType ~ ident ~ "," ~ keyVals =>
        val keys = keyVals.map(_._1).getOrElse(List.empty)
        val values = keyVals.flatMap(_._2).getOrElse(List.empty)
        new CollectionDeclaration(collectionType, ident, keys, values)
    })
  def columnsDeclaration: Parser[List[String]] = "[" ~> rep1sep(tableColumn, ",") <~ "]"

  def tableColumn = ident  // TODO

  /********** Statements **********/

  def statement = {
    def lhs = ident
    def rhs = ident
    lhs ~ bloomOp ~ rhs <~ eol
  }

  // Collection methods

  def program = rep(statement | collectionDeclaration | eol)

}


object BudParsersMain extends BudParsers {
  def main(args: Array[String]) {
    val p =
      """
      nodelist <= connect { |c| [c.client, c.nick] }
      mcast <~ (mcast * nodelist).pairs { |m,n| [n.key, m.val] }
      """.stripMargin
    val result = parseAll(program, p)
    println("The results are: " + result)
  }
}