package edu.berkeley.cs.boom.bloomscala.parser

import org.kiama.output.PrettyPrinter
import edu.berkeley.cs.boom.bloomscala.ast._
import edu.berkeley.cs.boom.bloomscala.typing._

/**
 * Pretty-printer for Bloom ASTs.
 */
object BloomPrettyPrinter extends PrettyPrinter {

  def toDoc(typ: BloomType): Doc = {
    typ match {
      case FunctionType(argTypes, returnType, properties) =>
          parens(ssep(argTypes.map(toDoc), comma <> space)) <+> "->" <+> toDoc(returnType)
      case param: TypeParameter =>
        param.name
      case FieldType(name) =>
        name
      case RecordType(fieldTypes) =>
        brackets(ssep(fieldTypes.map(toDoc), comma <> space))
      case UnknownType() =>
        "??"
    }
  }

  def toDoc(prop: FunctionProperty): Doc = {
    prop.getClass.getSimpleName
  }

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

  def pretty(typ: BloomType): String = {
    super.pretty(toDoc(typ))
  }

  def pretty(node: Node): String = {
    super.pretty(toDoc(node))
  }
}
