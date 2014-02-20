package edu.berkeley.cs.boom.bloomscala.parser

import org.kiama.output.PrettyPrinter
import edu.berkeley.cs.boom.bloomscala.ast._
import edu.berkeley.cs.boom.bloomscala.typing._

/**
 * Pretty-printer for Bloom ASTs.
 */
object BloomPrettyPrinter extends PrettyPrinter {

  implicit def toDoc(typ: BloomType): Doc = {
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

  implicit def toDoc(field: Field): Doc = {
    field.name <> colon <+> toDoc(field.typ)
  }

  implicit def toDoc(decl: CollectionDeclaration): Doc = {
    val values =
      if (decl.values.isEmpty) empty
      else space <> "=>" <+> brackets(ssep(decl.values.map(toDoc), comma <> space))
    decl.collectionType.toString.toLowerCase <+>
    decl.name <> comma <+>
    brackets(ssep(decl.keys.map(toDoc), comma <> space)) <> values
  }

  implicit def toDoc(prop: FunctionProperty): Doc = {
    prop.getClass.getSimpleName
  }

  implicit def toDoc(node: Node): Doc = {
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

  def pretty[T](x: T)(implicit toDoc: T => Doc): String = {
    super.pretty(toDoc(x))
  }
}
