package edu.berkeley.cs.boom.bloomscala.parser

import org.kiama.output.PrettyPrinter
import edu.berkeley.cs.boom.bloomscala.ast._

/**
 * Pretty-printer for Bloom ASTs.
 */
object BloomPrettyPrinter extends PrettyPrinter {
  def toDoc(node: Node): Doc = {
    node match {
      case PlusStatement(a, b, _) =>
        toDoc(a) <+> plus <+> toDoc(b)
      case RowExpr(colExprs) =>
        brackets(ssep(colExprs.map(toDoc), comma <> space))
      case EqualityPredicate(a, b) =>
        toDoc(a) <+> equal <+> toDoc(b)
      case fr: FieldRef =>
        fr.collection.name <> dot <> fr.fieldName
      case fc: FunctionCall =>
        fc.functionRef.name <> parens(ssep(fc.arguments.map(toDoc), comma <> space))
      case fr: FunctionRef =>
        fr.name
      case node: Node => node.toString
    }
  }

  def pretty(node: Node): String = {
    super.pretty(toDoc(node))
  }
}
