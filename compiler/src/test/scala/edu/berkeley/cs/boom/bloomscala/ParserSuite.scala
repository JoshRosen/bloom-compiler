package edu.berkeley.cs.boom.bloomscala

import edu.berkeley.cs.boom.bloomscala.parser.BudParser

class ParserSuite extends BloomScalaSuite {
  test("parse program containing only top-level declarations") {
    val program = BudParser.parseProgram(
      """
        | table a, [val: int]
        | table b, [val: int]
        | a <= b
      """.stripMargin)
    program.statements.size should be (1)
    program.declarations.size should be (2)
  }

  test("parse program containing only module and class declarations") {
    val program = BudParser.parseProgram(
      """
        | module myModule {
        |   state {
        |     table a, [val: int]
        |     table b, [val: int]
        |   }
        |   bloom {
        |     a <= b
        |   }
        | }
        | class myClass {
        |   state {
        |     table a, [val: int]
        |     table b, [val: int]
        |   }
        | }
      """.stripMargin)
    // TODO: there should probably be assertions here, but for now this test is just
    // checking that the parser doesn't crash.  The program above may be semantically invalid.
  }

  test("parse program containing only bloom and state blocks") {
    val program = BudParser.parseProgram(
      """
        | bloom {
        |   table a, [val: int]
        |   table b, [val: int]
        |   a <= b
        | }
        | bloom myBloom {
        |   table a, [val: int]
        |   table b, [val: int]
        |   a <= b
        | }
        | state { }
        | bloom { }
      """.stripMargin)
    // TODO: there should probably be assertions here, but for now this test is just
    // checking that the parser doesn't crash.  The program above may be semantically invalid.
  }

  test("parse program containing both modules, blocks, and top-level statements") {
    val program = BudParser.parseProgram(
      """
        | bloom {
        |   table a, [val: int]
        |   table b, [val: int]
        |   a <= b
        | }
        | class myClass {
        |   bloom {
        |     a <= b
        |   }
        | }
        | table a, [val: int]
        | a <= b
      """.stripMargin)
    // TODO: there should probably be assertions here, but for now this test is just
    // checking that the parser doesn't crash.  The program above may be semantically invalid.
  }

}
