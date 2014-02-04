package edu.berkeley.cs.boom.bloomscala


class NamerSuite extends BloomScalaSuite {

  test("Referencing undeclared collections should fail") {
    intercept[CompilerException] { Compiler.compileToIntermediateForm("lhs <= rhs") }
  }

  test("Referencing non-tuple variables in map functions should fail") {
    intercept[CompilerException] { Compiler.compileToIntermediateForm(
      """
        | table apple, [val: int]
        | apple <= apple { |a| [apple.int] }
      """.stripMargin)
    }
  }

}
