package edu.berkeley.cs.boom.bloomscala.typing

import org.kiama.attribution.Attribution._
import org.kiama.util.Messaging
import edu.berkeley.cs.boom.bloomscala.parser.AST
import AST._
import edu.berkeley.cs.boom.bloomscala.typing.FieldType._


class Typer(val messaging: Messaging) {

  import messaging.message

  def expectType(x: ColExpr, t: FieldType) {
    if (x->typ != t) message(x, s"Expected $t but got ${x->typ}")
  }

  lazy val typ: ColExpr => FieldType =
    attr {
      case PlusStatement(a, b) =>
        expectType(a, BloomInt)
        expectType(b, BloomInt)
        BloomInt
      case BoundFieldRef(_, _, field) =>
        field.typ
    }

  lazy val rhsSchema: StatementRHS => RecordType =
    attr {
      case mc: MappedCollection =>
        RecordType(mc.rowExpr.cols.map(_->typ))
      case mej: MappedEquijoin =>
        RecordType(mej.rowExpr.cols.map(_->typ))
      case cr: CollectionRef =>
        cr.collection.schema
      case notin @ NotIn(a, b) =>
        if (a.collection.schema != b.collection.schema)
          message(notin, s"notin called with incompatible schemas:\n${a.collection.schema}\n${b.collection.schema}")
        a.collection.schema
    }

  lazy val isWellTyped: Statement => Boolean =
    attr {
      case stmt @ Statement(lhs, op, rhs, _) =>
        val lSchema = lhs.collection.schema
        val rSchema = rhsSchema(rhs)
        if (rSchema != lSchema) {
          message(stmt, s"RHS has wrong schema; expected $lSchema but got $rSchema")
          false
        } else {
          true
        }
    }

}
