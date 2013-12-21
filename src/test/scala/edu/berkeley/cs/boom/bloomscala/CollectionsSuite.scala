package edu.berkeley.cs.boom.bloomscala

import org.scalatest.FunSuite
import edu.berkeley.cs.boom.bloomscala.collections.{Table, Scratch}

class BabyBud {
  implicit val bud = new Bud()
  val scratch = new Scratch[(String, String, Int, Int)]
  val scratch2 = new Scratch[(String, String)]
  val table = new Table[(String, String, Int, Int)]

  bud.addStrata(
    Seq(
      scratch <= ("a", "b", 1, 2),
      scratch <= ("a", "c", 3, 4),
      scratch2 <= ("a", "b"),
      table <= ("a", "b", 1, 2),
      table <= ("x", "y", 9, 8)

      //scratch <+ ("c", "d", 5, 6),
      //table <+ ("c", "d", 5, 6),
      //table <- ("a", "b", 1, 2),
    )
  )
}


class CollectionsSuite extends FunSuite {

  test("simple deduction") {
    val program = new BabyBud()
    program.bud.tick()
    assert(program.scratch.size === 2)
    assert(program.scratch2.size === 1)
    assert(program.table.size === 2)
  }
}