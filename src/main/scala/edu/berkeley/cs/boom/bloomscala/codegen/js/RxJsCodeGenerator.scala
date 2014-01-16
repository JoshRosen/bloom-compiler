package edu.berkeley.cs.boom.bloomscala.codegen.js

import edu.berkeley.cs.boom.bloomscala.Compiler
import Compiler.stratifier._
import Compiler.namer._
import edu.berkeley.cs.boom.bloomscala.parser.AST._

import edu.berkeley.cs.boom.bloomscala.parser.AST.CollectionDeclaration
import edu.berkeley.cs.boom.bloomscala.parser.AST.Program
import org.kiama.rewriting.Rewriter._
import scala.collection.immutable


object RxJsCodeGenerator extends org.kiama.output.PrettyPrinter {
  type NameResolver = Map[CollectionDeclaration, String]

  // Helper functions for performing rewriting prior to code generation:
  case class MappedJoin(a: CollectionRef,
                        b: CollectionRef,
                        predicate: Predicate,
                        shortNames: List[String],
                        colExprs: List[ColExpr]) extends DerivedCollection

  val joinMapConsolidation =
    rule {
      case MappedCollection(JoinedCollection(a, b, predicate), shortNames, colExprs) =>
        MappedJoin(a, b, predicate, shortNames, colExprs)
    }


  def gen(nameResolver: NameResolver)(node: Node): Doc =
    node match {

      case FieldRef(collection, field) =>
        nameResolver(collection) <> brackets(collection.indexOfField(field).toString)

      case cr: CollectionRef =>
        nameResolver(cr)

      case PlusStatement(a, b) =>
        gen(nameResolver)(a) <+> plus <+> gen(nameResolver)(b)

      case mc @ MappedJoin(a, b, EqualityPredicate(aExpr, bExpr), shortNames, colExprs) =>
        val newNameResolver: NameResolver = Map(
          collectionDeclaration(a) -> shortNames(0),
          collectionDeclaration(b) -> shortNames(1)
        )
        nameResolver(a) <> dot <> "join" <> parens( group( nest( ssep(immutable.Seq(
          text(nameResolver(b)),
          "function" <> parens("x") <+> braces( "return" <+> gen(newNameResolver)(aExpr) <> semi),
          "function" <> parens("x") <+> braces( "return" <+> gen(newNameResolver)(bExpr) <> semi),
          "function" <> parens(shortNames(0) <> comma <+> shortNames(1)) <+> braces(
            "return" <+> brackets(colExprs.map(gen(newNameResolver)).reduce(_ <> comma <+> _)) <> semi
          )), comma <+> line)
        )))

      case mc @ MappedCollection(cr: CollectionRef, shortNames, colExprs) =>
        val newNameResolver: NameResolver = Map(collectionDeclaration(cr) -> shortNames(0))
        nameResolver(cr) <> dot <> "map" <> parens("function" <> parens(shortNames.head) <+> braces(
          "return" <+> brackets(colExprs.map(gen(newNameResolver)).reduce(_ <> comma <+> _)) <> semi
        ))
    }

  def generateCode(program: Program) {
    val rewrittenProgram: Program = rewrite(outermost(joinMapConsolidation))(program)
    val stratifiedRules = rewrittenProgram.statements.groupBy(ruleStratum).toSeq.sortBy(x => x._1)
    for ((stratumNumber, rules) <- stratifiedRules) {
      val rulesByCollection = rules.groupBy(x => collectionDeclaration(x.lhs))
      for ((collection, rules) <- rulesByCollection) {
        val nameResolver = rewrittenProgram.declarations.map(x => (x, s"${x.name}Delta")).toMap
        val deltaComponents = for ((rule, index) <- rules.toSeq.zipWithIndex) yield {
          "var" <+> s"${collection.name}Delta$index" <+> equal <+> gen(nameResolver)(rule.rhs) <> semi
        }
        val delta = (1 to deltaComponents.size).map(index => text(s"${collection.name}Delta$index")).reduce(_ <> dot <> "union" <> parens(_))
        val deltaBlock = deltaComponents.reduce(_ <> linebreak <> _) <> linebreak <> s"var ${collection.name}Delta" <+> equal <+> delta
        println(super.pretty(deltaBlock))
      }
    }
  }
}
