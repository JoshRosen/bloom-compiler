package edu.berkeley.cs.boom.bloomscala.codegen.js

import edu.berkeley.cs.boom.bloomscala.parser.AST._
import scala.collection.immutable
import edu.berkeley.cs.boom.bloomscala.rewriting.DeltaForm
import edu.berkeley.cs.boom.bloomscala.analysis.{DepAnalyzer, Stratifier}

/**
 * Compiles Bloom programs to Javascript that use the RxJs and IxJS libraries.
 *
 * The output contains a Javascript object that exposes Bloom collections as
 * objects that implement the Rx Observable and Observer interfaces.
 *
 * myObj.collectionNameIn is an Observer that can be used to push new tuples
 * into the system by calling myObj.collectionNameIn.onNext(tuple).
 *
 * myObj.collectionNameOut is a Subject that can be subscribed() to
 * in order to receive callbacks when new tuples are added to collections.
 */
object RxJsCodeGenerator extends org.kiama.output.PrettyPrinter {

  // TODO: mangle collection names so as not to conflict with Javascript reserved words.
  // The naming code isn't 100% centralized here, so I'll have to go back through later
  // to clean things up.
  def name(cr: CollectionRef) = {
    cr match {
      case DeltaCollectionRef(_, collection) => s"${collection.name}Delta"
      case BoundCollectionRef(_, collection) => s"${collection.name}Table"
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

    val newDeltaNames = rules.map(r => r.lhs.collection.name).toSet ++ Set(deltaCollection.name)
    val generateNewDeltas = rules.map { case Statement(lhs, op, rhs) =>
      name(lhs) + "New" <+> equal <+> genRHS(rhs) <> dot <> "union" <> parens(name(lhs) + "New") <> semi
    }

    s"if (!${deltaCollection.name}Delta.isEmpty())" <+> braces(nest(linebreak <>
      newDeltaNames.map(name => text(s"var ${name}DeltaNew = Ix.Enumerable.empty();")).reduce(_ <@@> _) <@@>
      generateNewDeltas.reduce(_ <@@> _) <@@>
      s"${deltaCollection.name}Table = ${deltaCollection.name}Table.union(${deltaCollection.name}Delta);" <@@>
      s"${deltaCollection.name}Delta.forEach(function(x) { outerThis.${deltaCollection.name}Out.onNext(x); });" <@@>
      newDeltaNames.map(name => text(s"${name}Delta = ${name}DeltaNew.except(${name}Table);")).reduce(_ <@@> _)
    ) <> linebreak)
  }

  // TODO: eventually, this needs to take temporal rules into account.
  def generateCode(program: Program, stratifier: Stratifier, depAnalyzer: DepAnalyzer): String = {
    import depAnalyzer._
    import stratifier._

    // Declare collections:
    val subjectDeclarations = program.declarations.map { c =>
      s"""
        | this.${c.name}In = new Rx.Subject();
        | this.${c.name}In.subscribe(function(x) {
        |   ${c.name}Delta = ${c.name}Delta.union(Ix.Enumerable.fromArray([x]));
        |   handleDeltas();
        | });
        | this.${c.name}Out = new Rx.Subject();
      """.stripMargin.lines.filterNot(_.isEmpty).map(text).reduce(_ <@@> _)
    }
    val tableDeclarations =
      program.declarations.map(c => text(s"var ${c.name}Table = Ix.Enumerable.empty();"))
    val deltaDeclarations =
      program.declarations.map(c => text(s"var ${c.name}Delta = Ix.Enumerable.empty();"))

    val stratifiedCollections = program.declarations.groupBy(collectionStratum).toSeq.sortBy(x => x._1)

    // TODO: this needs to properly take stratification into account.
    val fixpointFunctionBody: Seq[Doc] =
      stratifiedCollections.flatMap { case (stratumNumber, collections) =>
        // Translate the rules to delta forms:
        val deltaRules = collections.flatMap(collectionStatements).flatMap(DeltaForm.toDeltaForm)
        // Group the rules by which delta they consume:
        val deltaConsumers =
          for ((collection, rules) <- deltaRules.groupBy(_._1)) yield {
            generateDeltaConsumerFunctionBody(collection.collection, rules.map(_._2).toSet)
          }
        deltaConsumers.toSeq
      }

    val fixpointNotReached = program.declarations.map(c => text(s"!${c.name}Delta.isEmpty()"))
      .reduce(_ <+> "||" <+> _)

    val code = "function Bloom ()" <+> braces(nest(
      linebreak <>
      "var outerThis = this;" <@@>
      empty <@@>
      subjectDeclarations.reduce(_ <@@> _) <@@>
      empty <@@>
      tableDeclarations.reduce(_ <@@> _) <@@>
      empty <@@>
      deltaDeclarations.reduce(_ <@@> _)  <@@>
      empty <@@>
      "function handleDeltas() " <+> braces(nest(
        linebreak <>
        "while" <+> parens(fixpointNotReached) <+> braces(nest(
          linebreak <>
          fixpointFunctionBody.reduce(_ <@@> _)
        ) <> linebreak)
      ) <> linebreak)
    ) <> linebreak)
    super.pretty(code)
  }
}
