package edu.berkeley.cs.boom.bloomscala



class TyperSuite extends BloomScalaSuite {

  test("NotIn schemas should match") {
    intercept[CompilerException] { Compiler.compileToIntermediateForm(
      """
        | table a, [val: int]
        | table b, [val: string]
        | a <= a.notin(b)
      """.stripMargin)
    }
  }

  test("NotIn LHS and RHS schemas should match") {
    intercept[CompilerException] { Compiler.compileToIntermediateForm(
      """
        | table a, [val: int]
        | table b, [val: int]
        | table c, [val: string]
        | c <= a.notin(b)
      """.stripMargin)
    }
  }

  test("Typing 3-way join") {
    Compiler.compileToIntermediateForm(
      """
        | table a, [val: int]
        | table b, [val: int]
        | table c, [val: int]
        | c <= (a * b * c) on (a.val == b.val, b.val == c.val) { |x, y, z| [x.val + y.val + z.val] }
      """.stripMargin)
  }
}
