package edu.berkeley.cs.boom.bloomscala

import org.scalatest.FunSuite

class NamerSuite extends FunSuite {

  test("Referencing undeclared collections should fail") {
    intercept[CompilerException] { Compiler.compile("lhs <= rhs") }
  }

  test("Referencing non-tuple variables in map functions should fail") {
    intercept[CompilerException] { Compiler.compile(
      """
        | table apple, [val: int]
        | apple <= apple { |a| [apple.int] }
      """.stripMargin)
    }
  }

}
