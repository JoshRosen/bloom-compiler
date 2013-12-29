package edu.berkeley.cs.boom.bloomscala

import shapeless._
import shapeless.syntax.std.tuple._
import shapeless.ops.hlist.{NatTRel, ToArray, Prepend}
import shapeless.test.illTyped

class MyTable[
  KeyShape <: HList,
  ValueShape <: HList,
  Shape <: HList]
  (name: String, keyShape: KeyShape, valueShape: ValueShape)
  (implicit prepend: Prepend.Aux[KeyShape, ValueShape, Shape],
   lub: ToArray[Shape, MyColumn[_]]) {

  val shape: Shape = keyShape ::: valueShape

  val columns: Array[MyColumn[_]] = shape.toArray

  //def <= [RowRep <: HList](record: RowRep)(implicit evidence: NatTRel[RowRep, MyRep, Shape, MyColumn]) {}
  def <= [RowRep <: HList](record: RowRep)(implicit evidence: NatTRel[RowRep, Id, Shape, MyColumn]) {}

}

class MyColumn[T](name: String) {}

class MyRep[T]

object ShapeExperiment {
  def main(args: Array[String]) {
    val shape = (new MyColumn[Int]("age"), new MyColumn[String]("name")).productElements
    val myTable = new MyTable("users", shape, shape)
    illTyped {"""new MyTable("users", shape, 1 :: shape)"""}
    //myTable <= (new MyRep[Int] :: new MyRep[String] :: new MyRep[Int] :: new MyRep[String] :: HNil)
    myTable <= (1 :: "Hello" :: 2 :: "World" :: HNil)

    illTyped {"""myTable <= (new MyRep[Int] :: HNil)"""}
  }
}
