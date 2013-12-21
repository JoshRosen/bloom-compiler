import scalaz._
import Scalaz._

object ShortestPaths {
  val link: Table[(Char, Char, Int)] = ???
  val path: Table[(Char, Char, Char, Int)] = ???
  //val shortest: Table[(Char, Char, Char, Int)] = ???

  val j = link.join(path)(1)


  val rules = Seq[Rule](
    link <= ('a', 'b', 1),
    link <= ('a', 'b', 3),
    link <= ('b', 'c', 1),
    link <= ('c', 'd', 1),
    link <= ('d', 'e', 1),
    path <= link.map {x => (x._1, x._2, x._2, x._3)},
    path <= j.map { case (l, p) =>
      (l._1, p._2, p._1, p._4 + l._3)
    }//,
    //shortest <= path
  )

  val bud = new Bud(rules)

  def main(args: Array[String]): Unit = {
    println("Hello World")
  }
}