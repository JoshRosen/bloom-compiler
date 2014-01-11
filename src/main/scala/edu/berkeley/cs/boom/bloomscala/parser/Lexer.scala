package edu.berkeley.cs.boom.bloomscala.parser

import scala.util.parsing.combinator.lexical.StdLexical


class Lexer extends StdLexical {
  delimiters += ( "(" , ")" , "," , "@", "[", "]", ".", "=>", "{", "}", "|", ":", "*", "==", "+")
  // TODO: leaving this out should produce an error message, but instead it silently
  // fails by building an alternatives() parser that matches nothing.
  reserved ++= CollectionType.nameToType.keys
  reserved ++= FieldType.nameToType.keys
  reserved ++= Seq("on", "notin")
  delimiters ++= BloomOp.symbolToOp.keys
}
