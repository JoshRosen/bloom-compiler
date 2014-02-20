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

  test("argmin ordering type must unify with ordering field type") {
    intercept[CompilerException] { Compiler.compileToIntermediateForm(
      """
        | table a, [key: int, val: int]
        | table b, [key: int, val: int]
        | b <= a.argmin([a.key], a.val, stringOrder)
      """.stripMargin)
    }
  }

  test("cannot insert into inputs") {
    intercept[CompilerException] { Compiler.compileToIntermediateForm(
      """
        | input a, [key: int, val: int]
        | table b, [key: int, val: int]
        | a <= b
      """.stripMargin)
    }
  }

  test("cannot read from outputs") {
    intercept[CompilerException] { Compiler.compileToIntermediateForm(
      """
        | output a, [key: int, val: int]
        | table b, [key: int, val: int]
        | b <= a
      """.stripMargin)
    }
  }
}
