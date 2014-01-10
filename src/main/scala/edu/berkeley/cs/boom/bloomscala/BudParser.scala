package edu.berkeley.cs.boom.bloomscala

import scala.util.parsing.input.Positional
import edu.berkeley.cs.boom.bloomscala.BloomCollectionType.BloomCollectionType
import scala.util.parsing.combinator.lexical.StdLexical
import scala.util.parsing.combinator.syntactical.StandardTokenParsers
import edu.berkeley.cs.boom.bloomscala.BloomFieldType.BloomFieldType


case class CollectionDeclaration(
    collectionType: BloomCollectionType,
    name: String,
    keys: List[Field],
    values: List[Field])
  extends Positional {
  val schema: List[BloomFieldType.BloomFieldType] = (keys ++ values).map(_.typ)
  def getField(name: String): Option[Field] = {
    (keys ++ values).find(_.name == name)
  }
}

/** Valid RHS's of statements */
sealed trait StatementRHS
sealed trait DerivedCollection extends StatementRHS with MappedCollectionTarget
sealed trait MappedCollectionTarget

case class MappedCollection(collection: MappedCollectionTarget, shortNames: List[String], colExprs: List[ColExpr]) extends DerivedCollection with Positional {
  var schema: Option[List[BloomFieldType]] = None
}
case class JoinedCollection(a: CollectionRef, b: CollectionRef, predicate: Any) extends DerivedCollection

case class CollectionRef(name: String) extends MappedCollectionTarget with Positional
case class Field(name: String, typ: BloomFieldType) extends Positional
case class FieldRef(collectionName: String, fieldName: String) extends ColExpr

abstract class ColExpr extends Positional {
  var typ: Option[BloomFieldType] = None
}
case class PlusStatement(lhs: ColExpr, rhs: ColExpr) extends ColExpr

case class Statement(lhs: Any, op: BloomOp.BloomOp, rhs: StatementRHS) extends Positional

case class EqualityPredicate[T](a: T, b: T) extends Positional

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
  delimiters += ( "(" , ")" , "," , "@", "[", "]", ".", "=>", "{", "}", "|", ":", "*", "==", "+")
  // TODO: leaving this out should produce an error message, but instead it silently
  // fails by building an alternatives() parser that matches nothing.
  reserved ++= BloomCollectionType.nameToType.keys
  reserved ++= BloomFieldType.nameToType.keys
  reserved += "on"
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

  def collectionRef = positioned(ident ^^ CollectionRef)


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

  def colTerm = fieldRef
  def colExpr: Parser[ColExpr] = {
    def plus = colTerm ~ "+" ~ colExpr ^^ {case a ~ "+" ~ b => PlusStatement(a, b)}
    plus | colTerm
  }
  def predicate = colExpr ~ "==" ~ colExpr ^^ { case a ~ "==" ~ b => EqualityPredicate(a, b)}

  def statement = {
    def lhs = ident
    def rhs = collectionMap | join

    def collection = collectionRef | derivedCollection
    def derivedCollection = join

    // i.e. (link * path) on (link.to == path.from)
    def join = "(" ~ collectionRef ~ "*" ~ collectionRef ~ ")" ~ "on" ~ "(" ~ predicate ~ ")" ^^ {
      case "(" ~ a ~ "*" ~ b ~ ")" ~ "on" ~ "(" ~ predicate ~ ")" =>
        JoinedCollection(a, b, predicate)
    }

    def collectionMap = positioned(collection ~ ("{" ~> "|" ~> rep1sep(ident, ",") <~ "|") ~ listOf(colExpr) <~ "}" ^^ {
      case collection ~ collectionShortNames ~ colExprs =>
        new MappedCollection(collection, collectionShortNames, colExprs)
    })
    positioned(lhs ~ bloomOp ~ rhs ^^ { case l ~ o ~ r => Statement(l, o, r)})
  }


  def program = rep(statement | collectionDeclaration)

}

object BudParsers extends BudParsers {
  def parseProgram(str: String): List[Either[CollectionDeclaration, Statement]] = {
    val tokens = new lexical.Scanner(str)
    val result = phrase(program)(tokens)
    result.get.map {
      case colDecl: CollectionDeclaration => Left(colDecl)
      case stmt: Statement => Right(stmt)
    }
  }
}


object BudParsersMain extends BudParsers {
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
    val result = BudParsers.parseProgram(p)
    import sext._
    println("The results are:\n" + result.treeString)
  }
}