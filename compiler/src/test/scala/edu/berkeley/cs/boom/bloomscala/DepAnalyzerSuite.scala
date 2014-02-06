package edu.berkeley.cs.boom.bloomscala

import edu.berkeley.cs.boom.bloomscala.analysis.{Dependency, DepAnalyzer}

class DepAnalyzerSuite extends BloomScalaSuite {

  test("Statements with only out edges should not participate in deductive cycles") {
    val program = Compiler.compileToIntermediateForm(
      """
        |      table a, []
        |      table b, []
        |      table c, []
        |
        |
        |      a <= b
        |      b <= a
        |      b <= c
      """.stripMargin)
    val depAnalyzer = new DepAnalyzer(program)
    import depAnalyzer._
    assert(participatesInDeductiveCycle(program.statements.toSeq(0)))
    assert(participatesInDeductiveCycle(program.statements.toSeq(1)))
    assert(!participatesInDeductiveCycle(program.statements.toSeq(2)))
  }

  test("Statements with only in edges should not participate in deductive cycles") {
    val program = Compiler.compileToIntermediateForm(
      """
        |      table a, []
        |      table b, []
        |      table c, []
        |
        |
        |      a <= b
        |      b <= a
        |      c <= b
      """.stripMargin)
    val depAnalyzer = new DepAnalyzer(program)
    import depAnalyzer._
    assert(participatesInDeductiveCycle(program.statements.toSeq(0)))
    assert(participatesInDeductiveCycle(program.statements.toSeq(1)))
    assert(!participatesInDeductiveCycle(program.statements.toSeq(2)))
  }

  test("Isolated statements should not participate in deductive cycles") {
    val program = Compiler.compileToIntermediateForm(
      """
        |      table a, []
        |      table b, []
        |      table c, []
        |      table d, []
        |
        |
        |
        |      a <= b
        |      b <= a
        |      c <= d
      """.stripMargin)
    val depAnalyzer = new DepAnalyzer(program)
    import depAnalyzer._
    assert(participatesInDeductiveCycle(program.statements.toSeq(0)))
    assert(participatesInDeductiveCycle(program.statements.toSeq(1)))
    assert(!participatesInDeductiveCycle(program.statements.toSeq(2)))
  }

  test("Temporal cycles are not declarative cycles") {
    val program = Compiler.compileToIntermediateForm(
      """
        |      table a, []
        |      table b, []
        |
        |      a <= b
        |      b <+ a
      """.stripMargin)
    val depAnalyzer = new DepAnalyzer(program)
    import depAnalyzer._
    assert(!participatesInDeductiveCycle(program.statements.toSeq(0)))
    assert(!participatesInDeductiveCycle(program.statements.toSeq(1)))
  }

  test("Declarative cycles are detected properly") {
    val program = Compiler.compileToIntermediateForm(
      """
        |      table a, []
        |      table b, []
        |
        |      a <= b
        |      b <= a
      """.stripMargin)
    val depAnalyzer = new DepAnalyzer(program)
    import depAnalyzer._
    assert(participatesInDeductiveCycle(program.statements.toSeq(0)))
    assert(participatesInDeductiveCycle(program.statements.toSeq(1)))
  }

  test("Non-monotonic dependencies") {
    val program = Compiler.compileToIntermediateForm(
      """
        |     table a, [key: int, val: int]
        |     table b, [ley: int, val: int]
        |     b <= a.choose([a.key], min(a.val))
      """.stripMargin
    )
    val depAnalyzer = new DepAnalyzer(program)
    import depAnalyzer._
    val statementDeps = program.statements.head->statementDependencies
    assert(statementDeps.exists { case Dependency(collection, isNegated, isTemporal, isMonotonic, _) =>
     collection.name == "a" && !isMonotonic
    })
  }

}

