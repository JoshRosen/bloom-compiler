package scala.edu.berkeley.cs.boom.bloomscala

import org.scalatest.FunSuite
import edu.berkeley.cs.boom.bloomscala.CompilerException
import edu.berkeley.cs.boom.bloomscala.Compiler


class TyperSuite extends FunSuite {

  test("Referencing undeclared collections should fail") {
    intercept[CompilerException] { Compiler.compile("lhs <= rhs") }
  }

}
