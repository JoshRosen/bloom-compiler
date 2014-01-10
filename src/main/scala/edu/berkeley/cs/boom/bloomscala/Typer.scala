package edu.berkeley.cs.boom.bloomscala

import com.typesafe.scalalogging.slf4j.Logging
import edu.berkeley.cs.boom.bloomscala.parser._
import edu.berkeley.cs.boom.bloomscala.parser.FieldType._
import edu.berkeley.cs.boom.bloomscala.parser.FieldType.FieldType
import edu.berkeley.cs.boom.bloomscala.parser.FieldRef
import edu.berkeley.cs.boom.bloomscala.parser.MappedCollection
import scala.Some
import edu.berkeley.cs.boom.bloomscala.parser.PlusStatement


class Typer(analysisInfo: AnalysisInfo) extends Logging with CompilerUtils {
  def getType(colExpr: ColExpr)(implicit collectionInfo: CollectionInfo): FieldType =  {
    colExpr.typ match {
      case Some(typ) => typ
      case None =>
        colExpr.typ = colExpr match {
          case PlusStatement(a, b) =>
            ensure(getType(a) == BloomInt, "Plus operand is not int", a.pos)
            ensure(getType(b) == BloomInt, "Plus operand is not int", a.pos)
            Some(BloomInt)
          case fieldRef: FieldRef =>
            val collection = collectionInfo(fieldRef.collectionName)(fieldRef.pos)
            fieldRef.collectionName = collection.name  // Resolve aliases
            val field = collection.getField(fieldRef.fieldName)
            ensure(field.isDefined,
              s"Collection ${fieldRef.collectionName}} doesn't have field ${fieldRef.fieldName}",
              fieldRef.pos)
            Some(field.get.typ)
        }
        colExpr.typ.get
    }
  }

  def getSchema(mc: MappedCollection): List[FieldType] = mc match {
    case MappedCollection(collection, shortNames, colExprs) =>
      mc.schema match {
        case Some(schema) => schema
        case None =>
          // Record how to resolve the short names:
          implicit val scopedCollections = analysisInfo.collections.clone
          collection match {
            case cref: CollectionRef =>
              ensure(shortNames.size == 1, "Too many short names", mc.pos)
              scopedCollections.alias(shortNames.head, analysisInfo.collections(cref))
            case jc: JoinedCollection =>
              ensure(shortNames.size == 2, "Too many short names", mc.pos)
              scopedCollections.alias(shortNames(0), analysisInfo.collections(jc.a))
              scopedCollections.alias(shortNames(1), analysisInfo.collections(jc.b))
          }
          // Determine the result type:
          mc.schema = Some(mc.colExprs.map(getType))
          mc.schema.get
      }
  }

  def run() {
    analysisInfo.parseResults foreach {
      case Left(colDecl) => analysisInfo.collections.declare(colDecl)
      case Right(stmt) =>
        logger.debug(s"Processing statement $stmt")
        val lhsCollection = analysisInfo.collections(stmt.lhs)
        stmt.rhs match {
          case mc: MappedCollection =>
            val schema = getSchema(mc)
            ensure(schema == lhsCollection.schema,
              s"RHS has wrong schema; expected ${lhsCollection.schema} but got ${mc.schema.get}", mc.pos)
        }
    }
  }
}
