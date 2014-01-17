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
      case field: FieldRef =>
        field.typ
    }

  lazy val rhsSchema: StatementRHS => List[FieldType] =
    attr {
      case mc: MappedCollection =>
        mc.colExprs.map(_->typ)
      case mej: MappedEquijoin =>
        mej.colExprs.map(_->typ)
      case cr: CollectionRef =>
        cr.schema
      case notin @ NotIn(a, b) =>
        if (a.schema != b.schema)
          message(notin, s"notin called with incompatible schemas:\n${a.schema}\n${b.schema}")
        a.schema
    }

  lazy val isWellTyped: Statement => Boolean =
    attr {
      case stmt @ Statement(lhs, op, rhs) =>
        val lSchema = lhs.schema
        val rSchema = rhsSchema(rhs)
        if (rSchema != lSchema) {
          message(stmt, s"RHS has wrong schema; expected $lSchema but got $rSchema")
          false
        } else {
          true
        }
    }

}
