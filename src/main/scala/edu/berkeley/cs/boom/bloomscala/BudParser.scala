package edu.berkeley.cs.boom.bloomscala

import scala.util.parsing.input.Positional
import edu.berkeley.cs.boom.bloomscala.BloomCollectionType.BloomCollectionType
import scala.util.parsing.combinator.lexical.StdLexical
import scala.util.parsing.combinator.syntactical.StandardTokenParsers


case class CollectionDeclaration(
    collectionType: BloomCollectionType,
    name: String,
    keys: List[Field],
    values: List[Field])
  extends Positional

case class Field(name: String, typ: BloomFieldType.BloomFieldType) extends Positional

case class FieldRef(collectionName: String, fieldName: String) extends Positional

case class MappedCollection(collectionName: String, shortName: String, colExprs: List[Any])

case class Statement(lhs: Any, op: BloomOp.BloomOp, rhs: Any)

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

object BloomFieldType extends Enumeration {
  type BloomFieldType = Value
  val BloomInt, BloomString = Value
  val nameToType: Map[String, BloomFieldType] = Map(
    "int" -> BloomInt,
    "string" -> BloomString
  )
}

class BudLexer extends StdLexical {
  delimiters += ( "(" , ")" , "," , "@", "[", "]", ".", "=>", "{", "}", "|", ":")
  // TODO: leaving this out should produce an error message, but instead it silently
  // fails by building an alternatives() parser that matches nothing.
  reserved ++= BloomCollectionType.nameToType.keys
  reserved ++= BloomFieldType.nameToType.keys
  delimiters ++= BloomOp.symbolToOp.keys
}

trait BudParsers extends StandardTokenParsers {

  override val lexical = new BudLexer


  /********** Helper Methods **********/

  def alternatives[T](map: Map[String, T], failMsg: String) =
    (map.keySet.map(keyword).reduce(_ | _) | failure(failMsg)) ^^ map.apply

  def listOf[T](t: Parser[T]): Parser[List[T]] = "[" ~> repsep(t, ",") <~ "]"

  def collectionType = alternatives(BloomCollectionType.nameToType, "Invalid collection type")
  def bloomOp = alternatives(BloomOp.symbolToOp, "Invalid operator")
  def bloomFieldType = alternatives(BloomFieldType.nameToType, "Invalid field type")


  /********** Declarations **********/

  def collectionDeclaration = {
    def columnsDeclaration: Parser[List[Field]] = listOf(tableColumn)
    def tableColumn = ident ~ ":" ~ bloomFieldType ^^ { case i ~ ":" ~ f => Field(i, f) }

    positioned((collectionType ~ ident ~ "," ~ opt(columnsDeclaration ~ opt("=>" ~> columnsDeclaration))) ^^ {
      case collectionType ~ ident ~ "," ~ keyVals =>
        val keys = keyVals.map(_._1).getOrElse(List.empty)
        val values = keyVals.flatMap(_._2).getOrElse(List.empty)
        new CollectionDeclaration(collectionType, ident, keys, values)
    })
  }


  /********** Statements **********/

  def fieldRef = (ident ~ "." ~ ident) ^^ {
    case collectionName ~ "." ~ field =>
      FieldRef(collectionName, field)
  }
  def colExpr = fieldRef | ident

  def statement = {
    def lhs = ident
    def rhs = collectionMap
    def collectionMap = ident ~ ("{" ~> "|" ~> ident <~ "|") ~ listOf(colExpr) <~ "}" ^^ {
      case collectionName ~ collectionShortName ~ colExprs =>
        new MappedCollection(collectionName, collectionShortName, colExprs)
    }
    lhs ~ bloomOp ~ rhs ^^ { case l ~ o ~ r => Statement(l, o, r)}
  }

  // Collection methods



  def program = rep(statement | collectionDeclaration)


}


object BudParsersMain extends BudParsers {
  def main(args: Array[String]) {
    val p =
      """
      table link, [from: string, to: string, cost: int]
      """.stripMargin
    val tokens = new lexical.Scanner(p)
    val result = phrase(program)(tokens)
    import sext._
    println("The results are:\n" + result.treeString)
  }
}