package edu.berkeley.cs.boom.bloomscala.codegen.js

import edu.berkeley.cs.boom.bloomscala.parser.AST._
import scala.collection.immutable
import edu.berkeley.cs.boom.bloomscala.analysis.{DepAnalyzer, Stratifier}
import edu.berkeley.cs.boom.bloomscala.codegen.CodeGenerator

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
object RxJsCodeGenerator extends CodeGenerator {

  // TODO: mangle collection names so as not to conflict with Javascript reserved words.
  // The naming code isn't 100% centralized here, so I'll have to go back through later
  // to clean things up.
  def name(cr: CollectionRef) = {
    cr match {
      case BoundCollectionRef(_, collection) => collection.name
    }
  }

  def genLambda(colExpr: ColExpr, varName: String): Doc = {
    val bindings: PartialFunction[CollectionDeclaration, String] = { case _ => varName}
    "function" <> parens(varName) <+> braces ( space <> "return" <+> genColExpr(colExpr, bindings) <> semi <> space)
  }

  def genLambda(rowExpr: RowExpr, varName: String): Doc = {
    val bindings: PartialFunction[CollectionDeclaration, String] = { case _ => varName}
    val newRow = brackets(rowExpr.cols.map(genColExpr(_, bindings)).reduce(_ <> comma <+> _))
    "function" <> parens(varName) <+> braces (space <> "return" <+> newRow <> semi <> space)
  }

  def methodCall(target: Doc, methodName: Doc, args: Doc*): Doc = {
    val argsSeq = immutable.Seq(args).flatten
    target <> dot <> methodName <> parens(group(nest(ssep(argsSeq, comma <> line))))
  }

  def functionCall(functionName: Doc, args: Doc*): Doc = {
    val argsSeq = immutable.Seq(args).flatten
    functionName <> parens(group(nest(ssep(argsSeq, comma <> line))))
  }

  /**
   * Translate the RHS of a statement into an Ix query.
   */
  def genRHS(rhs: StatementRHS): Doc = {
    rhs match {

      case cr: CollectionRef =>
        name(cr)

      case MappedEquijoin(a, b, aExpr, bExpr, tupVars, rowExpr) =>
        val bindings = Map(a.collection -> tupVars(0), b.collection -> tupVars(1))
        val newRow = brackets(rowExpr.cols.map(genColExpr(_, bindings)).reduce(_ <> comma <+> _))
        methodCall(name(a), "join",
          name(b),
          genLambda(aExpr, tupVars(0)),
          genLambda(bExpr, tupVars(1)),
          "function" <> parens(tupVars(0) <> comma <+> tupVars(1)) <+> braces(
            "return" <+> newRow <> semi
          ))

      case mc @ MappedCollection(cr: CollectionRef, tupVars, rowExpr) =>
        methodCall(name(cr), "map", genLambda(rowExpr, tupVars.head))
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

  def ruleFunctionParameters(stmt: Statement): Seq[CollectionDeclaration] = {
    stmt.rhs match {
      case cr: CollectionRef => Seq(cr.collection)
      case MappedCollection(r: CollectionRef, _, _) => Seq(r.collection)
      case MappedEquijoin(a, b, _, _, _, _) => Seq(a.collection, b.collection)
    }
  }

  def generateDeltaConsumerFunctionBody(deltaCollection: CollectionDeclaration, rules: Set[Statement]): Doc = {
    if (rules.isEmpty) return empty

    val newDeltaNames = rules.map(r => r.lhs.collection.name).toSet ++ Set(deltaCollection.name)
    val generateNewDeltas = rules.map { case stmt @ Statement(lhs, op, rhs, _) =>
      val params = ruleFunctionParameters(stmt)
      // TODO: handle the case where the same table is referenced twice by a rule:
      val deltaParams = params.map { c =>
        if (c == deltaCollection) c.name + "Delta"
        else c.name + "Table"
      }.map(text)
      name(lhs) + "DeltaNew" <+> equal <+> functionCall(s"rule${stmt.number}", deltaParams: _*) <> dot <> "union" <> parens(name(lhs) + "DeltaNew") <> semi
    }

    s"if (!${deltaCollection.name}Delta.isEmpty())" <+> braces(nest(
      linebreak <>
      newDeltaNames.map(name => text(s"var ${name}DeltaNew = Ix.Enumerable.empty();")).reduce(_ <@@> _) <@@>
      generateNewDeltas.reduce(_ <@@> _) <@@>
      s"${deltaCollection.name}Table = ${deltaCollection.name}Table.union(${deltaCollection.name}Delta);" <@@>
      s"${deltaCollection.name}Delta.forEach(function(x) { outerThis.${deltaCollection.name}Out.onNext(x); });" <@@>
      newDeltaNames.map(name => text(s"${name}Delta = ${name}DeltaNew.except(${name}Table);")).reduce(_ <@@> _)
    ) <> linebreak)
  }

  def generateRuleFunction(stmt: Statement): Doc = {
    val functionParams = ruleFunctionParameters(stmt).map(_.name).mkString(", ")
    s"function rule${stmt.number}" <> parens(functionParams) <+> braces(nest(
      linebreak <>
      // TODO: it would be cool to pretty-print the user's Bloom rule here
      // as a Javascript comment.
      "return" <+> genRHS(stmt.rhs) <> semi
    ) <> linebreak) <> linebreak
  }

  // TODO: eventually, this needs to take temporal rules into account.
  def generateCode(program: Program, stratifier: Stratifier, depAnalyzer: DepAnalyzer): CharSequence = {
    import depAnalyzer._
    import stratifier._

    // Declare collections:
    val subjectDeclarations = program.declarations.map { c =>
      s"""
        | this.${c.name}In = new Rx.Subject();
        | this.${c.name}In.subscribe(function(x) {
        |   ${c.name}Delta = ${c.name}Delta.union(Ix.Enumerable.return(x));
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

    val ruleFunctions = program.statements.map(generateRuleFunction)

    // TODO: this needs to properly take stratification into account.
    val fixpointFunctionBody: Seq[Doc] =
      stratifiedCollections.flatMap { case (stratumNumber, collections) =>
        // For each rule, determine its dependencies.  For each dependency, group together
        // all of the rules that depend on it:
        val deltaRules = collections.flatMap(collectionStatements).flatMap { stmt =>
          ruleFunctionParameters(stmt).map((_, stmt))
        }.groupBy(_._1)
        // Group the rules by which delta they consume:
        val deltaConsumers =
          for ((collection, rules) <- deltaRules) yield {
            generateDeltaConsumerFunctionBody(collection, rules.map(_._2).toSet)
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
      ruleFunctions.reduce(_ <@@> _)  <@@>
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
