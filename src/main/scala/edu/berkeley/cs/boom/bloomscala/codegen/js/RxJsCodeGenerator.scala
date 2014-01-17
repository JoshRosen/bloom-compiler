package edu.berkeley.cs.boom.bloomscala.codegen.js

import edu.berkeley.cs.boom.bloomscala.Compiler
import Compiler.stratifier._
import Compiler.namer._
import Compiler.depAnalyzer._
import edu.berkeley.cs.boom.bloomscala.parser.AST._

import edu.berkeley.cs.boom.bloomscala.parser.AST.CollectionDeclaration
import edu.berkeley.cs.boom.bloomscala.parser.AST.Program
import scala.collection.immutable


// TODO: the logic for generating delta rules should be performed
// in a target-language agnostic manner in its own module.
object RxJsCodeGenerator extends org.kiama.output.PrettyPrinter {
  type NameResolver = Function[CollectionDeclaration, String]


  def gen(nameResolver: NameResolver)(node: Node): Doc =
    node match {

      case FieldRef(collection, field) =>
        nameResolver(collection) <> brackets(collection.indexOfField(field).toString)

      case cr: CollectionRef =>
        nameResolver(cr)

      case PlusStatement(a, b) =>
        gen(nameResolver)(a) <+> plus <+> gen(nameResolver)(b)

      case MappedEquijoin(a, b, aExpr, bExpr, shortNames, colExprs) =>
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

  def generateSemiNaiveStepFunctionBody(collection: CollectionDeclaration): Doc = {
    val rules = collectionStatements(collection)
    if (rules.isEmpty) return empty

    // TODO: Kiama's functions like vcat only accept immutable seqs, whereas the interface
    // seems like it would be cleaner if they accepted the generic collection.Seq interface.
    val inputs = immutable.Seq(rules.flatMap(referencedCollections).toSeq:_*)
    val newUpdates = for (deltaCollection <- inputs) yield {
      def nameResolver(cDecl: CollectionDeclaration) = {
        if (cDecl == deltaCollection) {
          s"${cDecl.name}DeltaIn"
        } else {
          cDecl.name
        }
      }
      val rulesNeedingDelta = rules.filter(r => referencedCollections(r.rhs).contains(deltaCollection))
      for ((rule, index) <- rulesNeedingDelta.toSeq.zipWithIndex) yield {
        val variableName = s"${collection.name}NewGiven${deltaCollection.name}Delta$index"
        (variableName, "var" <+> variableName <+> equal <+> gen(nameResolver)(rule.rhs) <> semi)
      }
    }
    val allNewNames = newUpdates.flatten.map(_._1).map(text)
    val newUpdateStatements = newUpdates.flatten.map(_._2)
    val netNew = s"var ${collection.name}New" <+> equal <+> allNewNames.reduceLeft(_ <> dot <> "union" <> parens(_))
    val delta = s"var ${collection.name}Delta" <+> equal <+> netNew <+> minus <+> collection.name

    vcat(newUpdateStatements) <> linebreak <> netNew <> linebreak <> delta
    //var allDeltaUpdateRules = deltaUpdates.reduce(_ <> linebreak <> linebreak <> _)

    //allDeltaUpdateRules =
    //<> linebreak <> s"var ${collection.name}New" <+> equal <+> delta
    //<> linebreak <> s"var ${collection.name}Delta = ${collection.name}New - ${collection.name};")
  }

  def generateCode(program: Program) {
    // TODO: eventually, this needs to take temporal rules into account.
    val stratifiedCollections = program.declarations.groupBy(collectionStratum).toSeq.sortBy(x => x._1)
    for ((stratumNumber, collections) <- stratifiedCollections) {
      for (collection <- collections) {
        println(super.pretty(generateSemiNaiveStepFunctionBody(collection)))
      }
    }
  }
}
