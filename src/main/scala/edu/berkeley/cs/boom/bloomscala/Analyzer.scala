package edu.berkeley.cs.boom.bloomscala

import scala.collection.{immutable, mutable}
import scala.util.parsing.input.Position
import com.typesafe.scalalogging.slf4j.Logging
import java.util.concurrent.atomic.AtomicInteger
import edu.berkeley.cs.boom.bloomscala.parser._
import edu.berkeley.cs.boom.bloomscala.parser.CollectionDeclaration
import scala.Some
import edu.berkeley.cs.boom.bloomscala.parser.Statement
import edu.berkeley.cs.boom.bloomscala.parser.PlusStatement
import edu.berkeley.cs.boom.bloomscala.parser.FieldType.FieldType
import edu.berkeley.cs.boom.bloomscala.parser.FieldType

trait AnalyzerUtils {

  def ensure(pred: Boolean, msg: => String, pos: Position) {
    if (!pred) fail(msg)(pos)
  }

  def fail(msg: String)(implicit pos: Position): Nothing = {
    System.err.println(s"${pos.line}:${pos.column} $msg:\n${pos.longString}")
    sys.exit(-1)
  }
}

// Contains data shared across compilation phases
private class AnalysisInfo(val parseResults: List[Either[CollectionDeclaration, Statement]]) {
  val collections = new CollectionInfo
}

/**
 * Helper class for managing declarations of collections and aliasing
 * of collection names in map and join.
 */
private class CollectionInfo extends AnalyzerUtils {
  private val declarations = new mutable.HashMap[String, CollectionDeclaration]
  def declare(decl: CollectionDeclaration) {
    alias(decl.name, decl)
  }
  def alias(name: String, decl: CollectionDeclaration) {
    declare(name, decl)
  }
  def declare(name: String, decl: CollectionDeclaration) {
    declarations.get(name) match {
      case Some(existingDeclaration) =>
        val pos = decl.pos
        val firstPos = existingDeclaration.pos
        fail(s"Collection ${name} declared twice; first declared at ${firstPos.line}:${firstPos.column}:\n${firstPos.longString}\nand re-declared at ${pos.line}:${pos.column}")(decl.pos)
      case None =>
        declarations(name) = decl
    }
  }
  def apply(collectionRef: CollectionRef): CollectionDeclaration = {
    apply(collectionRef.name)(collectionRef.pos)
  }
  def apply(name: String)(implicit pos: Position): CollectionDeclaration = {
    declarations.get(name) match {
      case Some(decl) => decl
      case None =>
        fail(s"Collection $name isn't declared")
    }
  }
  def toMap = declarations.toMap
  override def clone: CollectionInfo = {
    val cloned = new CollectionInfo
    cloned.declarations ++= this.declarations
    cloned
  }
}

class Analyzer(analysisInfo: AnalysisInfo) extends Logging with AnalyzerUtils {


  private val nextAnonCollectionId = new AtomicInteger(0)

  private def nameAnonCollection(prefix: String) = {
    "$" + prefix + nextAnonCollectionId.getAndIncrement
  }

  def determineColExprType(colExpr: ColExpr)(implicit collectionInfo: CollectionInfo): FieldType =  {
    colExpr.typ match {
      case Some(typ) => typ
      case None =>
        colExpr.typ = colExpr match {
          case PlusStatement(a, b) =>
            ensure(determineColExprType(a) == FieldType.BloomInt, "Plus operand is not int", a.pos)
            ensure(determineColExprType(b) == FieldType.BloomInt, "Plus operand is not int", a.pos)
            Some(FieldType.BloomInt)
          case fieldRef: FieldRef =>
            val collection = collectionInfo(fieldRef.collectionName)(fieldRef.pos)
            val field = collection.getField(fieldRef.fieldName)
            ensure(field.isDefined, s"Collection ${fieldRef.collectionName}} doesn't have field ${fieldRef.fieldName}", fieldRef.pos)
            Some(field.get.typ)
        }
        colExpr.typ.get
    }
  }

  def processMappedCollection(mc: MappedCollection) = mc match {
    case MappedCollection(collection, shortNames, colExprs) =>
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
      mc.schema = Some(mc.colExprs.map(determineColExprType))
      logger.debug(s"Map result schema is ${mc.schema.get}")
  }

  def analyze() {
    analysisInfo.parseResults foreach {
      case Left(colDecl) => analysisInfo.collections.declare(colDecl)
      case Right(stmt) =>
        logger.debug(s"Processing statement $stmt")
          val lhsCollection = analysisInfo.collections(stmt.lhs.asInstanceOf[String])(stmt.pos)
          stmt.rhs match {
            case mc: MappedCollection =>
              processMappedCollection(mc)
              ensure(mc.schema.get == lhsCollection.schema,
                s"RHS has wrong schema; expected ${lhsCollection.schema} but got ${mc.schema.get}", mc.pos)
          }
    }
  }
}


object AnalyzerMain {
  def main(args: Array[String]) {
    val p =
      """
      table link, [from: string, to: string, cost: int]
      table path, [from: string, to: string, nxt: string, cost: int]
      table shortest, [from: string, to: string] => [nxt: string, cost: string]
      // Recursive rules to define all paths from links
      // Base case: every link is a path
      path <= link {|l| [l.from, l.to, l.to, l.cost]}
      // Inductive case: make a path of length n+1 by connecting a link to a
      // path of length n
      path <= (link * path) on (link.to == path.from) { |l, p|
        [l.from, p.to, l.to, l.cost+p.cost]
      }
      """.stripMargin
    val parseResults = BudParser.parseProgram(p)
    val info = new AnalysisInfo(parseResults)
    val analysis = new Analyzer(info).analyze()
    import sext._
    println("The results are:\n" + analysis.valueTreeString)
  }
}
