package edu.berkeley.cs.boom.bloomscala.parser

import scala.util.parsing.combinator.syntactical.StandardTokenParsers


trait BudParser extends StandardTokenParsers {

  override val lexical = new Lexer

  /********** Helper Methods **********/

  def alternatives[T](map: Map[String, T], failMsg: String) =
    (map.keySet.map(keyword).reduce(_ | _) | failure(failMsg)) ^^ map.apply

  def listOf[T](t: Parser[T]): Parser[List[T]] = "[" ~> repsep(t, ",") <~ "]"

  def collectionType = alternatives(CollectionType.nameToType, "Invalid collection type")
  def bloomOp = alternatives(BloomOp.symbolToOp, "Invalid operator")
  def fieldType = alternatives(FieldType.nameToType, "Invalid field type")

  def collectionRef = positioned(ident ^^ CollectionRef)


  /********** Declarations **********/

  def collectionDeclaration = {
    def columnsDeclaration: Parser[List[Field]] = listOf(tableColumn)
    def tableColumn = ident ~ ":" ~ fieldType ^^ { case i ~ ":" ~ f => Field(i, f) }

    positioned((collectionType ~ ident ~ "," ~ opt(columnsDeclaration ~ opt("=>" ~> columnsDeclaration))) ^^ {
      case collectionType ~ ident ~ "," ~ keyVals =>
        val keys = keyVals.map(_._1).getOrElse(List.empty)
        val values = keyVals.flatMap(_._2).getOrElse(List.empty)
        new CollectionDeclaration(collectionType, ident, keys, values)
    })
  }


  /********** Statements **********/

  def fieldRef = positioned((ident ~ "." ~ ident) ^^ {
    case collectionName ~ "." ~ field =>
      FieldRef(collectionName, field)
  })

  def colTerm = fieldRef
  def colExpr: Parser[ColExpr] = {
    def plus = colTerm ~ "+" ~ colExpr ^^ {case a ~ "+" ~ b => PlusStatement(a, b)}
    plus | colTerm
  }
  def predicate = colExpr ~ "==" ~ colExpr ^^ { case a ~ "==" ~ b => EqualityPredicate(a, b)}

  def statement = {
    def lhs = collectionRef
    def rhs = collectionMap | join | collectionRef

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

object BudParser extends BudParser {
  def parseProgram(str: String): List[Either[CollectionDeclaration, Statement]] = {
    val tokens = new lexical.Scanner(str)
    val result = phrase(program)(tokens)
    result.get.map {
      case colDecl: CollectionDeclaration => Left(colDecl)
      case stmt: Statement => Right(stmt)
    }
  }
}