package edu.berkeley.cs.boom.bloomscala.codegen.js

import edu.berkeley.cs.boom.bloomscala.parser.AST._
import scala.collection.immutable
import edu.berkeley.cs.boom.bloomscala.rewriting.DeltaForm
import edu.berkeley.cs.boom.bloomscala.analysis.{DepAnalyzer, Stratifier}


object RxJsCodeGenerator extends org.kiama.output.PrettyPrinter {

  def name(cr: CollectionRef) = {
    cr match {
      case DeltaCollectionRef(_, collection) => s"${collection.name}Delta"
      case BoundCollectionRef(_, collection) => collection.name
    }
  }

  /**
   * Translate the RHS of a statement into an Ix query.
   */
  def genRHS(rhs: StatementRHS): Doc = {
    rhs match {

      case cr: CollectionRef =>
        name(cr)

      case MappedEquijoin(a, b, aExpr, bExpr, tupVars, colExprs) =>
        val bindings = Map(a.collection -> tupVars(0), b.collection -> tupVars(1))
        val newRow = brackets(colExprs.map(genColExpr(_, bindings)).reduce(_ <> comma <+> _))
        name(a) <> dot <> "join" <> parens( group( nest( ssep(immutable.Seq(
          text(name(b)),
          "function" <> parens(tupVars(0)) <+> braces( "return" <+> genColExpr(aExpr, bindings) <> semi),
          "function" <> parens(tupVars(1)) <+> braces( "return" <+> genColExpr(bExpr, bindings) <> semi),
          "function" <> parens(tupVars(0) <> comma <+> tupVars(1)) <+> braces(
            "return" <+> newRow <> semi
          )), comma <+> line)
        )))

      case mc @ MappedCollection(cr: CollectionRef, tupVars, colExprs) =>
        val bindings = Map(cr.collection -> tupVars.head)
        val newRow = brackets(colExprs.map(genColExpr(_, bindings)).reduce(_ <> comma <+> _))
        name(cr) <> dot <> "map" <> parens("function" <> parens(tupVars.head) <+> braces(
          "return" <+> newRow <> semi
        ))
    }
  }

  /**
   * Translate column expressions to statements appearing in UDF bodies.
   *
   * @param expr the expression to translate
   * @param bindings a mapping from collections to local variable names in the UDF.
   */
  def genColExpr(expr: ColExpr, bindings: PartialFunction[CollectionDeclaration, String]): Doc = {
    expr match {
      case cr: CollectionRef =>
        bindings(cr.collection)
      case BoundFieldRef(collectionRef, field, _) =>
        bindings(collectionRef.collection) <> brackets(collectionRef.collection.indexOfField(field).toString)
      case PlusStatement(a, b) =>
        genColExpr(a, bindings) <+> plus <+> genColExpr(b, bindings)
    }
  }

  def generateDeltaConsumerFunctionBody(deltaCollection: CollectionDeclaration, rules: Set[Statement]): Doc = {
    if (rules.isEmpty) return empty

    val updateDeltas = rules.map { case Statement(lhs, op, rhs) =>
      name(lhs) <+> equal <+> name(lhs) <> dot <> "union" <> parens(genRHS(rhs) <+> minus <+> name(lhs)) <> semi
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
