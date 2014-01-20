package edu.berkeley.cs.boom.bloomscala.codegen.js

import edu.berkeley.cs.boom.bloomscala.parser.AST._
import scala.collection.immutable
import edu.berkeley.cs.boom.bloomscala.rewriting.DeltaForm
import edu.berkeley.cs.boom.bloomscala.analysis.{DepAnalyzer, Stratifier}


// TODO: the logic for generating delta rules should be performed
// in a target-language agnostic manner in its own module.
object RxJsCodeGenerator extends org.kiama.output.PrettyPrinter {

  def name(cr: CollectionRef) = {
    cr match {
      case DeltaCollectionRef(_, collection) => s"${collection.name}Delta"
      case BoundCollectionRef(_, collection) => collection.name
    }
  }

  def gen(node: Node): Doc =
    node match {

      case BoundFieldRef(collectionRef, field, _) =>
        // Here, we use the "short name" alias that the user assigned:
        collectionRef.name <> brackets(collectionRef.collection.indexOfField(field).toString)

      case cr: CollectionRef =>
        name(cr)

      case PlusStatement(a, b) =>
        gen(a) <+> plus <+> gen(b)

      case MappedEquijoin(a, b, aExpr, bExpr, shortNames, colExprs) =>
        name(a) <> dot <> "join" <> parens( group( nest( ssep(immutable.Seq(
          text(name(b)),
          "function" <> parens(shortNames(0)) <+> braces( "return" <+> gen(aExpr) <> semi),
          "function" <> parens(shortNames(1)) <+> braces( "return" <+> gen(bExpr) <> semi),
          "function" <> parens(shortNames(0) <> comma <+> shortNames(1)) <+> braces(
            "return" <+> brackets(colExprs.map(gen).reduce(_ <> comma <+> _)) <> semi
          )), comma <+> line)
        )))

      case mc @ MappedCollection(cr: CollectionRef, shortNames, colExprs) =>
        name(cr) <> dot <> "map" <> parens("function" <> parens(shortNames.head) <+> braces(
          "return" <+> brackets(colExprs.map(gen).reduce(_ <> comma <+> _)) <> semi
        ))
    }

  def generateDeltaConsumerFunctionBody(deltaCollection: CollectionDeclaration, rules: Set[Statement]): Doc = {
    if (rules.isEmpty) return empty

    val updateDeltas = rules.map { case Statement(lhs, op, rhs) =>
      name(lhs) <+> equal <+> name(lhs) <> dot <> "union" <> parens(gen(rhs) <+> minus <+> name(lhs)) <> semi
    }

    s"if (!${deltaCollection.name}Delta.isEmpty())" <+> braces(nest(linebreak <>
      updateDeltas.reduce(_ <@@> _) <@@>
      deltaCollection.name <+> equal <+> deltaCollection.name <> dot <> "union" <> parens(s"${deltaCollection.name}Delta") <> semi <@@>
      s"${deltaCollection.name}Delta = Ix.Enumerable.empty();"
    ) <> linebreak)
  }

  def generateCode(program: Program, stratifier: Stratifier, depAnalyzer: DepAnalyzer) {
    import depAnalyzer._
    import stratifier._
    // TODO: eventually, this needs to take temporal rules into account.
    // Declare collections:
    // TODO: mangle collection names so as not to conflict with Javascript reserved words.
    val declarations =
      program.declarations.map(c => text(s"var ${c.name} = Ix.Enumerable.empty();")) ++
      program.declarations.map(c => text(s"var ${c.name}Delta = Ix.Enumerable.empty();"))
    println(super.pretty(declarations.reduce(_ <@@> _)))

    val stratifiedCollections = program.declarations.groupBy(collectionStratum).toSeq.sortBy(x => x._1)
    for ((stratumNumber, collections) <- stratifiedCollections) {
      // Translate the rules to delta forms:
      val deltaRules = collections.flatMap(collectionStatements).flatMap(DeltaForm.toDeltaForm)
      // Group the rules by which delta they consume:
      for ((collection, rules) <- deltaRules.groupBy(_._1)) {
        val body = generateDeltaConsumerFunctionBody(collection.collection, rules.map(_._2).toSet)
        println(super.pretty(body))
      }
    }
  }
}
