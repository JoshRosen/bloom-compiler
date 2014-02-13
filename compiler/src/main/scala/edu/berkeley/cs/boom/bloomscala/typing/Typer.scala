package edu.berkeley.cs.boom.bloomscala.typing

import org.kiama.attribution.Attribution._
import org.kiama.util.Messaging
import edu.berkeley.cs.boom.bloomscala.ast._
import edu.berkeley.cs.boom.bloomscala.typing.FieldType._
import org.kiama.rewriting.PositionalRewriter._


class Typer(messaging: Messaging) {

  import messaging.message

  /**
   * Assign types to all expressions.
   */
  def resolveTypes(program: Program): Program = {
    program.statements.map(isWellTyped)
    rewrite(everywherebu(assignType))(program)
  }

  private val assignType =
    rule {
      case ut: UnboundType =>
        // An unbound type should appear as a field of an Expr, so
        // grab that expression's type:
        ut.parent match {
          case ce: ColExpr =>
            colType(ce)
          case re: RowExpr =>
            rowType(re)
        }
    }

  private def expectType(x: ColExpr, t: BloomType) {
    if (x->colType != t) message(x, s"Expected $t but got ${x->colType}")
  }

  private lazy val colType: ColExpr => BloomType =
    attr {
      case PlusStatement(a, b, _) =>
        expectType(a, BloomInt)
        expectType(b, BloomInt)
        BloomInt
      case BoundFieldRef(_, _, field) =>
        field.typ
    }

  private lazy val rowType: RowExpr => RecordType =
    attr {
      case RowExpr(colExprs) =>
        RecordType(colExprs.map(_->colType))
    }

  private lazy val rhsSchema: StatementRHS => RecordType =
    attr {
      case mc: MappedCollection =>
        mc.rowExpr->rowType
      case join: JoinedCollections =>
        join.rowExpr->rowType
      case cr: CollectionRef =>
        cr.collection.schema
      case notin @ NotIn(a, b) =>
        if (a.collection.schema != b.collection.schema)
          message(notin, s"notin called with incompatible schemas:\n${a.collection.schema}\n${b.collection.schema}")
        a.collection.schema
      case choose @ ChooseCollection(collection, groupingCols, funcCall) =>
        if (groupingCols.map(_.field).toSet.size != groupingCols.size)
          message(choose, "Grouping columns cannot contain duplicates")
        val funcType = funcCall.functionRef.function.typ
        if (Unifier.unify(funcType, FunctionTypes.exemplaryAggregate).isFailure)
          message(choose, s"choose expected exemplary aggregate, but found function of type $funcType")
        collection.collection.schema
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
