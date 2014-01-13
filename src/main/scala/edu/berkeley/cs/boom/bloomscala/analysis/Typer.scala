package edu.berkeley.cs.boom.bloomscala.analysis

import org.kiama.attribution.Attribution._
import org.kiama.util.Messaging
import edu.berkeley.cs.boom.bloomscala.parser.AST._
import edu.berkeley.cs.boom.bloomscala.parser.AST.FieldType._


class Typer(val messaging: Messaging, namer: Namer) {

  import messaging.message
  import namer._

  def expectType(x: ColExpr, t: FieldType) {
    if (x->typ != t) message(x, s"Expected $t but got ${x->typ}")
  }

  lazy val typ: ColExpr => FieldType =
    attr {
      case PlusStatement(a, b) =>
        expectType(a, BloomInt)
        expectType(b, BloomInt)
        BloomInt
      case fieldRef: FieldRef =>
        (fieldRef->fieldDeclaration).typ
    }

  lazy val rhsSchema: StatementRHS => List[FieldType] =
    attr {
      case mc @ MappedCollection(collection, shortNames, colExprs) =>
        colExprs.map(_->typ)
      case notin @ NotIn(a, b) =>
        val aSchema = (a->collectionDeclaration).schema
        val bSchema = (b->collectionDeclaration).schema
        if (aSchema != bSchema)
          message(notin, s"notin called with incompatible schemas:\n$aSchema\n$bSchema")
        aSchema
    }

  lazy val isWellTyped: Statement => Boolean =
    attr {
      case stmt @ Statement(lhs, op, rhs) =>
        val lSchema = (lhs->collectionDeclaration).schema
        val rSchema = rhsSchema(rhs)
        if (rSchema != lSchema) {
          message(stmt, s"RHS has wrong schema; expected $lSchema but got $rSchema")
          false
        } else {
          true
        }
    }

}
