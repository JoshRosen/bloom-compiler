package edu.berkeley.cs.boom.bloomscala

import scala.collection.{immutable, mutable}
import scala.util.parsing.input.Position
import com.typesafe.scalalogging.slf4j.Logging
import java.util.concurrent.atomic.AtomicInteger
import edu.berkeley.cs.boom.bloomscala.BloomFieldType.BloomFieldType

class Analyzer(parseResults: List[Either[CollectionDeclaration, Statement]]) extends Logging {

  val collections = new mutable.HashMap[String, CollectionDeclaration]

  private val nextAnonCollectionId = new AtomicInteger(0)

  private def nameAnonCollection(prefix: String) = {
    "$" + prefix + nextAnonCollectionId.getAndIncrement
  }

  def ensure(pred: Boolean, msg: => String, pos: Position) {
    if (!pred) fail(msg, pos)
  }

  def fail(msg: String, pos: Position) {
    System.err.println(s"${pos.line}:${pos.column} $msg:\n${pos.longString}")
    sys.exit(-1)
  }

  def determineColExprType(colExpr: ColExpr)(implicit collectionInfo: Map[String, CollectionDeclaration]): BloomFieldType =  {
    colExpr.typ match {
      case Some(typ) => typ
      case None =>
        colExpr.typ = colExpr match {
          case PlusStatement(a, b) =>
            ensure(determineColExprType(a) == BloomFieldType.BloomInt, "Plus operand is not int", a.pos)
            ensure(determineColExprType(b) == BloomFieldType.BloomInt, "Plus operand is not int", a.pos)
            Some(BloomFieldType.BloomInt)
          case fieldRef: FieldRef =>
            val collection = collectionInfo.get(fieldRef.collectionName)
            collection.getOrElse(
              fail(s"Collection ${fieldRef.collectionName} isn't declared", fieldRef.pos))
            val field = collection.get.getField(fieldRef.fieldName)
            ensure(field.isDefined, s"Collection ${fieldRef.collectionName}} doesn't have field ${fieldRef.fieldName}", fieldRef.pos)
            Some(field.get.typ)
        }
        colExpr.typ.get
    }
  }

  def processMappedCollection(mc: MappedCollection) = mc match {
    case MappedCollection(collection, shortNames, colExprs) =>
      // Determine how to resolve the short names:
      val shortNameResolutions: Map[String, CollectionDeclaration] =
        collection match {
          case cref: CollectionRef => {
            ensure(shortNames.size == 1, "Too many short names", mc.pos)
            immutable.Map(shortNames.head -> collections(cref.name))
          }
          case jc: JoinedCollection =>
            ensure(shortNames.size == 2, "Too many short names", mc.pos)
            immutable.Map(shortNames(0) -> collections(jc.a.name),
                          shortNames(1) -> collections(jc.b.name))
        }
      // Push those resolutions into scope:
      implicit val scopedCollections = collections.toMap ++ shortNameResolutions
      logger.debug(s"Resolved short names as $shortNameResolutions")
      // Determine the result type:
      mc.schema = Some(mc.colExprs.map(determineColExprType))
      logger.debug(s"Map result schema is ${mc.schema.get}")
  }

  def analyze() {
    parseResults foreach {
      case Left(colDecl) =>
       if (collections.contains(colDecl.name)) {
         fail(s"Collection ${colDecl.name} declared twice", colDecl.pos)
       } else {
         logger.debug(s"Declaring collection ${colDecl.name}")
         collections(colDecl.name) = colDecl
       }
      case Right(stmt) =>
        logger.debug(s"Processing statement $stmt")
          val lhsCollection = collections.get(stmt.lhs.asInstanceOf[String])
          ensure(lhsCollection.isDefined, s"Collection ${stmt.lhs} isn't declared", stmt.pos)
          stmt.rhs match {
            case mc: MappedCollection =>
              processMappedCollection(mc)
              ensure(mc.schema.get == lhsCollection.get.schema,
                s"RHS has wrong schema; expected ${lhsCollection.get.schema} but got ${mc.schema.get}", mc.pos)
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
    val result = BudParsers.parseProgram(p)
    val analysis = new Analyzer(result).analyze()
    import sext._
    println("The results are:\n" + analysis.treeString)
  }
}